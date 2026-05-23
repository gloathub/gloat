# gloat-java-interop -- Calling Java from Gloat

## Synopsis

Gloat understands the standard Clojure JVM-interop idioms (`Math/sqrt`,
`Math/PI`, and so on) and routes them to a Go-native port of the JVM
runtime called [gojava](https://github.com/gloathub/gojava). The goal is
that unmodified `.clj` source written for Clojure-on-the-JVM behaves the
same way when compiled by gloat, even where Go's standard library would
give a different answer.

The supported surface today is `java.lang.Math`. Other classes follow
the same pattern as gojava grows.

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
2. **Bridge** -- a small Go package inside glojure
   (`pkg/javacompat/math/`) that handles JVM-specific semantics:
   polymorphic dispatch on argument type, JVM-faithful rounding rules,
   exact-arithmetic overflow detection.
3. **gojava** -- the typed Go port of `java.lang.*`, with one Go name
   per JVM overload (`AbsInt`, `AbsLong`, `FloorDivInt`, `FloorDivLong`,
   ...).

At the REPL the rewrite step is skipped; the bridge registers each Math
symbol under the JVM-style `Math.<name>` key in glojure's pkgmap so
`(Math/sqrt 144)` resolves the same way without translation.

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

## Supported symbols

All of `java.lang.Math`:

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

## Status

`java.lang.Math` is complete. `java.lang.String`, `java.lang.Integer`,
`java.lang.Float`, and related classes are on the roadmap and will
follow the same three-layer pattern. Until then, use the dot-method
forms (`(.toUpperCase s)`) on Go strings or Go-style package calls.
