# gloat-java-interop -- Calling Java from Gloat

## Synopsis

Gloat understands the standard Clojure JVM-interop idioms (`Math/sqrt`,
`Math/PI`, and so on) and routes them to a Go-native port of the JVM
runtime called [gojava](https://github.com/gloathub/gojava). The goal is
that unmodified `.clj` source written for Clojure-on-the-JVM behaves the
same way when compiled by gloat, even where Go's standard library would
give a different answer.

The supported surface today is `java.lang.Math`, `java.lang.System`,
`java.lang.Integer`, `java.lang.Long`, `java.lang.String`,
`java.lang.Double`, `java.lang.Boolean`, `java.lang.Character`,
`java.util.regex.Pattern` (with Matcher), and `java.util.UUID`.
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

Fully-qualified `java.lang.X/y` symbols are also accepted and resolve
identically to the bare form, mirroring standard Clojure where
`java.lang.*` is auto-imported. So `(java.lang.Math/abs -1)` and
`(Math/abs -1)` produce the same result. The same applies to
constructors: `(java.lang.Integer. 100)` and `(Integer. 100)` are
equivalent.

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

## java.lang.String

Runnable: [`08-string.clj`](../demo/interop/java-interop/08-string.clj)

`String/*` statics rewrite to the javacompat bridge like `Math/*` does.
Instance methods (`(.length s)`, `(.toUpperCase s)`, `(.substring s 1
4)`, ...) take a different path: glojure has no Go-side methods on the
primitive `string` type, so the bridge registers each JVM method with
the runtime, and `lang.FieldOrMethod` consults that registry when the
receiver is a Go string. The dispatch is case-insensitive on the first
letter so both the original (`.equals`) and the rewrite-renamed
(`.Equals`) spellings reach the same handler.

```clojure
(.length "naïve")              ; 5 (UTF-16 code units, JVM semantics)
(.toUpperCase "hello")         ; "HELLO"
(.substring "hello world" 6)   ; "world"
(.indexOf "hello" "lo")        ; 3
(.replace "foobar" "oo" "OO")  ; "fOObar"
(.split "a,b,c" ",")           ; ["a" "b" "c"]
(.hashCode "hello")            ; 99162322  (JVM int32 algorithm)
(.trim "  hi  ")               ; "hi"      (ASCII whitespace only)
(.strip "  hi  ")              ; "hi"      (Unicode whitespace)

(String/format "%s=%d" "x" 42) ; "x=42"
(String/join "-" ["a" "b"])    ; "a-b"
(String/valueOf nil)           ; "null"
(String. "hello")              ; "hello"   (rewrites to valueOf)
```

The format string for `String/format` accepts `%n` (newline), `%b`
(boolean as "true"/"false"), and positional `%N$x` indices in addition
to Go's standard verbs. Regex methods (`.matches`, `.replaceAll`,
`.replaceFirst`, `.split`) compile their pattern through Go's `regexp`
package; the supported syntax is Go's RE2, not Java's regex.

## java.lang.Double

Runnable: [`09-double.clj`](../demo/interop/java-interop/09-double.clj)

`Double/*` covers the static surface of the boxed double: parsing,
predicates, comparison, and bit-level conversion. Values are float64
to match Java's double precision.

```clojure
(Double/parseDouble "3.14")            ; 3.14
(Double/valueOf "2.5")                 ; 2.5  (string overload)
(Double/valueOf 4.0)                   ; 4.0  (numeric overload)

(Double/isNaN Double/NaN)              ; true
(Double/isInfinite Double/POSITIVE_INFINITY) ; true
(Double/isFinite 1.5)                  ; true

(Double/toString Double/MAX_VALUE)     ; "1.7976931348623157E308"
(Double/toString Double/NaN)           ; "NaN"
(Double/toString Double/POSITIVE_INFINITY) ; "Infinity"

(Double/max 3.0 7.0)                   ; 7.0
(Double/compare 1.0 2.0)               ; -1

(Double/doubleToLongBits 1.0)          ; 4607182418800017408
(Double/longBitsToDouble 4607182418800017408) ; 1.0

(Double. "1.25")                       ; 1.25 (rewrites to valueOf)
```

`Double/toString` is JVM-faithful: mantissa always carries a decimal
point (`"1.0"`, not `"1"`), the exponent is uppercase `E` with no `+`
sign on positive exponents, and the special values render as `"NaN"`,
`"Infinity"`, and `"-Infinity"`. Glojure's `println` formats float64
infinities as `##Inf` / `##-Inf`; route through `Double/toString` when
you want the JVM spelling.

## java.lang.Boolean

Runnable: [`10-boolean.clj`](../demo/interop/java-interop/10-boolean.clj)

`Boolean/*` exposes the static surface. `parseBoolean` and `valueOf`
follow the JVM's lenient rule: they return `true` only when the input
equals `"true"` ignoring case, and `false` for every other value
including the empty string and nil.

```clojure
(Boolean/parseBoolean "TRUE")          ; true   (case insensitive)
(Boolean/parseBoolean "yes")           ; false  (anything else)
(Boolean/valueOf "True")               ; true
(Boolean/valueOf true)                 ; true

(Boolean/toString true)                ; "true"
(Boolean/compare false true)           ; -1

(Boolean/logicalAnd true false)        ; false
(Boolean/logicalOr  true false)        ; true
(Boolean/logicalXor true true)         ; false

Boolean/TRUE                           ; true
Boolean/FALSE                          ; false

(Boolean. "true")                      ; true   (rewrites to valueOf)
```

The logical combinators are present so callers porting code that uses
them as point-free functions (e.g. `(reduce Boolean/logicalAnd ...)`)
don't have to rewrite to `(and ...)`. `Boolean/getBoolean` looks up a
system property by name; in gloat it falls back to the process
environment when no property is set.

## java.lang.Character

Runnable: [`11-character.clj`](../demo/interop/java-interop/11-character.clj)

`Character/*` covers the static predicates, case folding, and
radix-aware digit conversion. Glojure's character literal (`\a`, `\5`,
`\space`) parses to `lang.Char` (a rune wrapper); the bridge unwraps
it, so you can pass either a `\c` literal or a plain integer code
point.

```clojure
(Character/isDigit \5)                 ; true
(Character/isLetter \x)                ; true
(Character/isWhitespace \tab)          ; true
(Character/isAlphabetic \z)            ; true

(Character/toUpperCase \a)             ; \A
(Character/toLowerCase \Z)             ; \z
(Character/toString \k)                ; "k"

(Character/digit \f 16)                ; 15
(Character/digit \z 10)                ; -1  (out of radix)
(Character/forDigit 10 16)             ; \a
(Character/getNumericValue \7)         ; 7
(Character/compare \a \b)              ; -1

Character/MIN_RADIX                    ; 2
Character/MAX_RADIX                    ; 36

(Character. \W)                        ; \W   (rewrites to valueOf)
```

Classification uses Go's `unicode` package, which tracks the same
categories the JVM uses; predicates like `isLetter` and `isDigit`
match the JVM result for the BMP code points typically encountered.
`isWhitespace` follows the JVM's quirk of accepting tab/newline/
form-feed/carriage-return/`0x1c-1f` while explicitly *excluding* the
no-break spaces that `isSpaceChar` accepts.

## java.util.regex.Pattern

Runnable: [`12-regex.clj`](../demo/interop/java-interop/12-regex.clj)

`Pattern/*` covers the static factories, the Matcher API, and the
flag constants. The underlying engine is Go's `regexp` (RE2), so a
few JVM features are unavailable: possessive quantifiers (`*+`,
`++`, `?+`), backreferences (`\1`, `\k<name>`), and lookaround
(`(?=...)`, `(?!...)`). Java-style named groups (`(?<name>...)`) are
translated to RE2's `(?P<name>...)` form before compilation, so the
Java syntax works.

```clojure
(Pattern/matches "\\d+" "12345")           ; true

(let [p (Pattern/compile "(\\d+)-(\\w+)")
      m (.matcher p "42-foo")]
  (.find m)                                ; true
  (.group m)                               ; "42-foo"
  (.group m 1)                             ; "42"
  (.group m 2))                            ; "foo"

(let [p (Pattern/compile "HELLO" Pattern/CASE_INSENSITIVE)
      m (.matcher p "hello world")]
  (.find m))                               ; true

(let [p (Pattern/compile "\\s+")]
  (vec (.split p "foo  bar\tbaz")))        ; ["foo" "bar" "baz"]

(Pattern/quote "a.b*c")                    ; "a\\.b\\*c"
(.pattern (Pattern. "x+"))                 ; "x+"  (ctor sugar)
```

Matcher instance methods (`.find`, `.group`, `.start`, `.end`,
`.matches`, `.lookingAt`, `.replaceAll`, `.replaceFirst`,
`.groupCount`, `.reset`) reach through reflection on the Go `*Matcher`
receiver. The matcher is stateful between `.find` calls, mirroring
the JVM. Pattern flags (`CASE_INSENSITIVE`, `MULTILINE`, `DOTALL`,
`LITERAL`, `UNICODE_CASE`) compile down to the corresponding RE2
inline modifiers or to `regexp.QuoteMeta` for `LITERAL`.

## java.util.UUID

Runnable: [`13-uuid.clj`](../demo/interop/java-interop/13-uuid.clj)

`UUID/*` covers the random and name-based factories, parsing,
ordering, hashing, and the two-long constructor. Random UUIDs are
RFC 4122 v4 (`crypto/rand`); name-based UUIDs are v3 (MD5 of the
raw input bytes, matching `UUID.nameUUIDFromBytes` exactly).

```clojure
(let [u (UUID/randomUUID)]
  (.version u)                             ; 4
  (.variant u)                             ; 2
  (.toString u))                           ; "<hex>-<hex>-4<hex>...-..."

(let [u (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")]
  (.getMostSignificantBits u)              ; 81985529216486895
  (.getLeastSignificantBits u))            ; 81985529216486895

(.toString (UUID/nameUUIDFromBytes (.getBytes "hello")))
;; "5d41402a-bc4b-3a76-b971-9d911017c592"  (matches JVM)

(let [u (UUID. 42 99)]                     ; ctor sugar to fromBits
  (.toString u))                           ; "00000000-0000-002a-0000-000000000063"

(.compareTo (UUID. 1 2) (UUID. 1 3))       ; -1
(.hashCode (UUID/fromString "..."))        ; JVM XOR-and-fold algorithm
```

Instance methods reach through reflection on the Go `*UUID`
receiver. The Go type implements `fmt.Stringer`, so `(str u)` and
`println u` yield the canonical hex form. The fully qualified
`java.util.UUID/...` form also resolves through the bridge for most
calls; a few of clojure.core's internal `(java.util.UUID/fromString s)`
and `(java.util.UUID/randomUUID)` call sites still resolve to
glojure's existing `google/uuid` implementation for back-compat with
the stdlib `uuid?` predicate.

## java.lang.Thread

Runnable: [`14-thread.clj`](../demo/interop/java-interop/14-thread.clj)

Only `Thread/sleep` is wired today. The full Java Thread API (`start`,
`join`, `interrupt`, names, uncaught handlers) has no direct goroutine
analogue and is intentionally out of scope. `Thread/sleep` itself is
trivial: gojava forwards to `time.Sleep`.

```clojure
(Thread/sleep 50)                            ; sleep 50 ms
(Thread/sleep 1 500000)                      ; millis + nanos overload
(java.lang.Thread/sleep 1)                   ; fully qualified resolves
```

Negative millis or out-of-range nanoseconds panic, matching the JVM's
`IllegalArgumentException`. There is no `InterruptedException`: gojava
never interrupts a sleeping goroutine.

## java.time.Instant

Runnable: [`15-instant.clj`](../demo/interop/java-interop/15-instant.clj)

`Instant/*` covers the wall-clock factory (`now`), the ISO-8601 parser
(`parse`), the two epoch factories (`ofEpochSecond` 1+2-arg,
`ofEpochMilli`), and the `EPOCH` constant. Instance methods cover the
seconds/nanos accessors, the `toEpochMilli` reverse conversion,
comparison (`compareTo`, `equals`, `isBefore`, `isAfter`), `hashCode`,
and the six `plus`/`minus` arithmetic methods (`Seconds`, `Millis`,
`Nanos`).

```clojure
(.toString Instant/EPOCH)                  ; "1970-01-01T00:00:00Z"
(.toString (Instant/now))                  ; "2026-05-24T17:42:08.123Z"

(let [i (Instant/parse "2007-12-03T10:15:30.500Z")]
  (.getEpochSecond i)                      ; 1196676930
  (.getNano i)                             ; 500000000
  (.toEpochMilli i)                        ; 1196676930500
  (.toString (.plusMillis i 1500)))        ; "2007-12-03T10:15:32Z"

(.toString (Instant/ofEpochSecond 0 1500000000))
;; "1970-01-01T00:00:01.500Z"  (nano-adjustment normalised)

(.compareTo (Instant/parse "2007-12-03T10:15:30Z")
            (Instant/parse "2007-12-03T10:15:31Z"))   ; -1
```

`toString` matches Java's grouped output: no fraction when nanos is 0,
otherwise the shortest of 3 (millis), 6 (micros), or 9 (nanos) digits
that exactly represents the value. The Go type implements `fmt.Stringer`,
so `(str i)` and `println i` produce the same canonical form. Instant
has no public constructor on the JVM, so there is no `(Instant. ...)`
sugar.

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

### java.lang.String

- Statics: `String/format`, `String/join`, `String/valueOf`,
  `String/copyValueOf`
- Constructor: `(String. x)` (rewrites to `valueOf`)
- Length and predicates: `.length`, `.isEmpty`, `.isBlank`
- Case: `.toUpperCase`, `.toLowerCase`
- Trimming: `.trim`, `.strip`, `.stripLeading`, `.stripTrailing`
- Substrings: `.substring` (1+2 arg), `.charAt`, `.codePointAt`
- Search: `.indexOf`, `.lastIndexOf` (string or char, with optional
  from-index), `.startsWith` (1+2 arg), `.endsWith`, `.contains`
- Compare: `.equals`, `.equalsIgnoreCase`, `.compareTo`,
  `.compareToIgnoreCase`
- Build: `.concat`, `.repeat`, `.replace` (char or string),
  `.replaceAll`, `.replaceFirst`, `.matches`, `.split` (1+2 arg)
- Convert: `.toCharArray`, `.getBytes`, `.chars`, `.codePoints`,
  `.lines`, `.toString`, `.intern`
- Hash: `.hashCode` (JVM int32 algorithm)

### java.lang.Double

- Constants: `Double/MIN_VALUE`, `Double/MAX_VALUE`, `Double/MIN_NORMAL`,
  `Double/POSITIVE_INFINITY`, `Double/NEGATIVE_INFINITY`, `Double/NaN`,
  `Double/SIZE`, `Double/BYTES`
- Parsing: `Double/parseDouble`, `Double/valueOf` (string or number)
- Constructor: `(Double. x)` (rewrites to `valueOf`)
- Formatting: `Double/toString` (JVM-style), `Double/toHexString`
- Predicates: `Double/isNaN`, `Double/isInfinite`, `Double/isFinite`
- Bit conversion: `Double/doubleToLongBits`,
  `Double/doubleToRawLongBits`, `Double/longBitsToDouble`
- Comparisons: `Double/compare`, `Double/max`, `Double/min`, `Double/sum`

### java.lang.Boolean

- Constants: `Boolean/TRUE`, `Boolean/FALSE`
- Parsing: `Boolean/parseBoolean`, `Boolean/valueOf` (string or boolean)
- Constructor: `(Boolean. x)` (rewrites to `valueOf`)
- Formatting: `Boolean/toString`
- Logical: `Boolean/logicalAnd`, `Boolean/logicalOr`, `Boolean/logicalXor`
- Properties: `Boolean/getBoolean` (falls back to the process env)
- Comparison: `Boolean/compare`

### java.lang.Character

- Constants: `Character/MIN_VALUE`, `Character/MAX_VALUE`,
  `Character/MIN_RADIX`, `Character/MAX_RADIX`
- Constructor: `(Character. c)` (rewrites to `valueOf`)
- Predicates: `Character/isDigit`, `Character/isLetter`,
  `Character/isLetterOrDigit`, `Character/isAlphabetic`,
  `Character/isWhitespace`, `Character/isSpaceChar`,
  `Character/isUpperCase`, `Character/isLowerCase`
- Case folding: `Character/toUpperCase`, `Character/toLowerCase`
- Conversion: `Character/toString`, `Character/digit`,
  `Character/forDigit`, `Character/getNumericValue`
- Comparison: `Character/compare`

### java.util.regex.Pattern

- Statics: `Pattern/compile` (1+2 arg), `Pattern/matches`, `Pattern/quote`
- Constructor: `(Pattern. regex)` (rewrites to `compile`)
- Flag constants: `Pattern/CASE_INSENSITIVE`, `Pattern/MULTILINE`,
  `Pattern/LITERAL`, `Pattern/DOTALL`, `Pattern/UNICODE_CASE`
- Pattern instance: `.pattern`, `.flags`, `.toString`, `.matcher`,
  `.split` (1+2 arg), `.asPredicate`
- Matcher instance: `.matches`, `.lookingAt`, `.find` (0+1 arg),
  `.group` (0+1 arg, by index or name), `.start` (0+1 arg),
  `.end` (0+1 arg), `.groupCount`, `.replaceAll`, `.replaceFirst`,
  `.reset` (0+1 arg), `.pattern`

### java.util.UUID

- Statics: `UUID/randomUUID`, `UUID/fromString`, `UUID/nameUUIDFromBytes`
- Constructor: `(UUID. msb lsb)` (rewrites to `fromBits`)
- Instance: `.toString`, `.getMostSignificantBits`,
  `.getLeastSignificantBits`, `.version`, `.variant`, `.compareTo`,
  `.equals`, `.hashCode`

### java.lang.Thread

- Statics: `Thread/sleep` (1-arg millis, 2-arg millis+nanos)

Instance methods (`start`, `join`, `interrupt`, `getName`,
`currentThread`, ...) are not supported.

### java.time.Instant

- Statics: `Instant/now`, `Instant/parse`, `Instant/ofEpochSecond`
  (1+2 arg), `Instant/ofEpochMilli`
- Constant: `Instant/EPOCH`
- Instance: `.toString`, `.getEpochSecond`, `.getNano`,
  `.toEpochMilli`, `.compareTo`, `.equals`, `.isBefore`, `.isAfter`,
  `.hashCode`, `.plusSeconds`, `.plusMillis`, `.plusNanos`,
  `.minusSeconds`, `.minusMillis`, `.minusNanos`

## Status

`java.lang.Math`, `java.lang.System`, `java.lang.Integer`,
`java.lang.Long`, `java.lang.String`, `java.lang.Double`,
`java.lang.Boolean`, `java.lang.Character`, `java.lang.Thread`
(`sleep` only), `java.util.regex.Pattern` (with Matcher),
`java.util.UUID`, and `java.time.Instant` are usable. `java.lang.Class`,
`java.io.File`, `java.lang.Throwable`, and other commonly-used classes
are on the roadmap and will follow the same three-layer pattern.
