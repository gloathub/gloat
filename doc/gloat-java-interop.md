# gloat-java-interop -- Calling Java from Gloat

## Synopsis

Gloat understands the standard Clojure JVM-interop idioms (`Math/sqrt`,
`Math/PI`, and so on) and routes them to a Go-native port of the JVM
runtime called [gojava](https://github.com/gloathub/gojava). The goal is
that unmodified `.clj` source written for Clojure-on-the-JVM behaves the
same way when compiled by gloat, even where Go's standard library would
give a different answer.

The supported surface today is `java.lang.Math` and `java.lang.System`.
Other classes follow the same pattern as gojava grows.

```clojure
(ns main.core)

(defn -main []
  (println (Math/sqrt 144))            ; 12.0
  (println Math/PI)                    ; 3.141592653589793
  (println (Math/floorDiv -7 2)))      ; -4 (JVM floor semantics)
```

JVM-style and Go-style interop are additive: `(Math/sqrt x)` and
`(math.Sqrt x)` both compile and can appear in the same file.

## How it works

JVM symbols flow through three layers:

1. **Rewrite** -- the `.clj` -> `.glj` transform translates each
   `Math/*` symbol to a fully-qualified Go reference into the bridge
   package.
2. **Bridge** -- small Go packages inside glojure
   (`pkg/javacompat/math/`, `pkg/javacompat/system/`) that handle
   JVM-specific semantics: polymorphic dispatch on argument type,
   JVM-faithful rounding rules, exact-arithmetic overflow detection,
   nil-on-unset env/property lookups, conversion of Go return values
   into Clojure-friendly forms.
3. **gojava** -- the typed Go port of `java.lang.*`, with one Go name
   per JVM overload (`AbsInt`, `AbsLong`, `FloorDivInt`, `FloorDivLong`,
   ...).

At the REPL the rewrite step is skipped; each bridge registers its
symbols under the JVM-style `Math.<name>` and `System.<name>` keys in
glojure's pkgmap so `(Math/sqrt 144)` and `(System/getenv "PATH")`
resolve the same way without translation.

## Static method calls

Runnable: [`01-static-method.clj`](../demo/interop/java-interop/01-static-method.clj)

`Math.sqrt(144)` on the JVM is `(Math/sqrt 144)` in Glojure. No import
is needed. Single-argument methods are the most common case; two-arg
forms work the same way.

```clojure
(Math/sqrt 144)            ; 12.0
(Math/pow 2 10)            ; 1024.0
(Math/floor 2.7)           ; 2.0
(Math/ceil 2.3)            ; 3.0
```

## Static fields

Runnable: [`02-static-field.clj`](../demo/interop/java-interop/02-static-field.clj)

Static fields are referenced as `Class/NAME` in non-call position. The
existing constants are `Math/PI` and `Math/E`.

```clojure
Math/PI                    ; 3.141592653589793
Math/E                     ; 2.718281828459045
(Math/toRadians 180)       ; PI
```

## Polymorphic dispatch

Runnable: [`03-polymorphic-dispatch.clj`](../demo/interop/java-interop/03-polymorphic-dispatch.clj)

The JVM overloads `Math.abs` (and `min`, `max`, `floorDiv`, `floorMod`,
`*Exact`) on int, long, float, and double. Go has no overloading, so
gojava exposes typed names (`AbsInt`, `AbsLong`, etc.) and the bridge
picks the right one at runtime based on the actual value type. Integer
inputs return integers; double inputs return doubles.

```clojure
(Math/abs -42)             ; 42      (long stays long)
(Math/abs -3.5)            ; 3.5     (double stays double)
(Math/max 7 3)             ; 7
(Math/max 7.0 3)           ; 7.0     (any double -> double)
```

## JVM-faithful semantics

Runnable: [`04-jvm-semantics.clj`](../demo/interop/java-interop/04-jvm-semantics.clj)

Several `java.lang.Math` operations have specific rounding or overflow
behaviour that is **not** the same as what you would get from Go's
`math` package or its arithmetic operators. The bridge guarantees the
JVM result.

```clojure
(Math/round 2.5)           ; 3
(Math/round -2.5)          ; -2  (half-values round toward +Inf, JVM-style)
(Math/floorDiv -7 2)       ; -4  (floor semantics, vs Go's -7/2 = -3)
(Math/floorMod -7 2)       ; 1   (floor mod, vs Go's -7%2 = -1)
```

## Mixing with Go-style interop

Runnable: [`05-dual-interop.clj`](../demo/interop/java-interop/05-dual-interop.clj)

JVM-style and Go-style forms coexist:

```clojure
(Math/sqrt 144)            ; 12.0   (JVM-style; via the bridge)
(math.Sqrt 144)            ; 12.0   (Go stdlib; direct call)
(Math/abs -42)             ; 42     (polymorphic, returns long)
(math.Abs -42.0)           ; 42.0   (Go's math.Abs takes float64 only)
```

Use the JVM form when you want JVM-faithful semantics or when you are
porting Clojure source. Use the Go form when you want to call the Go
stdlib directly (or any third-party Go package).

## java.lang.System

Runnable: [`06-system.clj`](../demo/interop/java-interop/06-system.clj)

The supported surface covers time, environment variables, system
properties, process exit, the standard streams, and a couple of
housekeeping helpers.

```clojure
(System/currentTimeMillis)            ; ms since epoch (long)
(System/nanoTime)                     ; high-resolution time source (long)

(System/getenv "PATH")                ; value, or nil if unset
(System/getenv)                       ; full env as a Clojure map

(System/getProperty "user.home")      ; value, or nil
(System/getProperty "x" "fallback")   ; value, or "fallback"
(System/setProperty "k" "v")          ; returns previous value or nil
(System/clearProperty "k")            ; returns previous value or nil

(System/lineSeparator)                ; "\n" on Unix, "\r\n" on Windows
(System/gc)                           ; runs Go's runtime.GC()
(System/exit 0)                       ; calls os.Exit
```

`System/getenv` follows JVM semantics: a missing variable returns `nil`,
not the empty string. `System/getProperty` works the same way, and the
two-argument form returns its default.

The property table is seeded with the JVM-style keys most often read by
Clojure code: `user.home`, `user.dir`, `os.name`, `file.separator`,
`line.separator`, `path.separator`. Use `setProperty` to extend it.

### Standard streams

`System/out`, `System/err`, and `System/in` are thin wrappers over
`os.Stdout`, `os.Stderr`, and `os.Stdin`. They expose the most-reached-for
methods of `java.io.PrintStream` / `java.io.InputStream`. Glojure's
instance-method resolution capitalizes the first letter, so JVM-style
calls land on the Go method:

```clojure
(.println System/out "hello")         ; -> os.Stdout.Println
(.print   System/err "oops")          ; -> os.Stderr.Print
(.printf  System/out "%d\n" 42)       ; -> os.Stdout.Printf
(.flush   System/out)                 ; -> os.Stdout.Flush (best-effort)
(.read    System/in  buf)             ; -> os.Stdin.Read
```

Note that idiomatic Clojure code usually goes through `(println ...)`
and friends (which route via `*out*`), not the streams directly. The
streams are here for code that asks for them by name.

### Not yet supported

`arraycopy`, `identityHashCode`, the security-manager / finalization
APIs. Open an issue if you have a Clojure codebase that needs any of
these.

## java.lang.Integer and java.lang.Long

Runnable: [`07-integer-long.clj`](../demo/interop/java-interop/07-integer-long.clj)

The boxed integer classes carry a fixed-width contract from the JVM:
`Integer` is 32 bits, `Long` is 64 bits.
That contract is preserved here, so `Integer/MAX_VALUE` is
`2147483647` (not Go's `math.MaxInt`, which is 2^63-1 on 64-bit
platforms) and `Integer/parseInt` will refuse values that overflow
int32.

```clojure
(Integer/parseInt "42")               ; -> 42 (int32)
(Integer/parseInt "1010" 2)           ; -> 10 (radix parsing)
(Integer/parseInt "ff" 16)            ; -> 255
(Long/parseLong "9999999999")         ; -> 9999999999 (int64)

Integer/MAX_VALUE                     ; -> 2147483647
Long/MAX_VALUE                        ; -> 9223372036854775807

(Integer/toBinaryString 42)           ; -> "101010"
(Integer/toHexString 255)             ; -> "ff"
(Long/toHexString 4096)               ; -> "1000"

(Integer/valueOf 7)                   ; -> 7
(Integer/valueOf "7")                 ; -> 7 (parses)
(Integer. 5)                          ; rewrites to (Integer/valueOf 5)
(Long.    "9999999999")               ; rewrites to (Long/valueOf "...")

(Integer/bitCount 0xFF)               ; -> 8
(Integer/numberOfLeadingZeros 1)      ; -> 31
(Integer/signum -5)                   ; -> -1
(Integer/max 3 7)                     ; -> 7
```

Constructor forms `(Integer. x)` and `(Long. x)` are rewritten at
compile time to the matching `valueOf`, which accepts either a number
(coerced to int32 / int64) or a string (parsed as decimal). Parse
failures raise a runtime error with `NumberFormatException` in the
message, matching JVM behavior.

## Supported symbols

### java.lang.Math (complete)

- Constants: `Math/PI`, `Math/E`
- Powers and roots: `Math/sqrt`, `Math/cbrt`, `Math/pow`, `Math/exp`,
  `Math/expm1`, `Math/log`, `Math/log10`, `Math/log1p`
- Trigonometry: `Math/sin`, `Math/cos`, `Math/tan`, `Math/asin`,
  `Math/acos`, `Math/atan`, `Math/atan2`, `Math/sinh`, `Math/cosh`,
  `Math/tanh`
- Conversion: `Math/toRadians`, `Math/toDegrees`
- Rounding: `Math/floor`, `Math/ceil`, `Math/round`, `Math/rint`,
  `Math/signum`
- Sign/magnitude: `Math/abs`, `Math/copySign`, `Math/hypot`,
  `Math/IEEEremainder`
- Min/max: `Math/min`, `Math/max`
- Floor div/mod: `Math/floorDiv`, `Math/floorMod`
- Exact arithmetic: `Math/addExact`, `Math/subtractExact`,
  `Math/multiplyExact`, `Math/negateExact`, `Math/incrementExact`,
  `Math/decrementExact`, `Math/toIntExact`
- Random: `Math/random`

### java.lang.System

- Time: `System/currentTimeMillis`, `System/nanoTime`
- Environment: `System/getenv` (0-arg map, 1-arg lookup)
- Properties: `System/getProperty` (1+2 arg), `System/setProperty`,
  `System/clearProperty`
- Lifecycle: `System/exit`, `System/gc`
- Misc: `System/lineSeparator`
- Streams: `System/out`, `System/err`, `System/in` (with `.println`,
  `.print`, `.printf`, `.flush`, `.write`, `.read`)

### java.lang.Integer

- Constants: `Integer/MIN_VALUE`, `Integer/MAX_VALUE`, `Integer/SIZE`,
  `Integer/BYTES`
- Parsing: `Integer/parseInt` (1+2 arg), `Integer/parseUnsignedInt`,
  `Integer/valueOf` (int or string)
- Constructor: `(Integer. x)` (rewrites to `valueOf`)
- Formatting: `Integer/toString` (1+2 arg), `Integer/toBinaryString`,
  `Integer/toOctalString`, `Integer/toHexString`
- Bit operations: `Integer/bitCount`, `Integer/numberOfLeadingZeros`,
  `Integer/numberOfTrailingZeros`, `Integer/highestOneBit`,
  `Integer/lowestOneBit`, `Integer/reverse`, `Integer/reverseBytes`
- Comparisons: `Integer/compare`, `Integer/max`, `Integer/min`,
  `Integer/signum`, `Integer/sum`

### java.lang.Long

Same surface as `java.lang.Integer`, with `Long/` prefixes and int64
return types. Includes `Long/MIN_VALUE`, `Long/MAX_VALUE`,
`Long/parseLong`, `Long/valueOf`, `Long/toBinaryString`,
`Long/toHexString`, `Long/bitCount`, etc.

## Status

`java.lang.Math`, `java.lang.System`, `java.lang.Integer`, and
`java.lang.Long` are usable.
`java.lang.String`, `java.lang.Double`, `java.lang.Boolean`,
`java.lang.Character`, and related classes are on the roadmap and will
follow the same three-layer pattern. Until then, use the dot-method
forms (`(.toUpperCase s)`) on Go strings or Go-style package calls.
