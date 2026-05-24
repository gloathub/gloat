Java Interop Examples
=====================

Short, runnable Clojure programs that demonstrate calling `java.lang.*`
from Glojure. Today the supported surface is `java.lang.Math`,
`java.lang.System`, `java.lang.Integer`, `java.lang.Long`,
`java.lang.String`, `java.lang.Double`, `java.lang.Boolean`, and
`java.lang.Character`; the pattern generalises to other classes as the
[gojava](https://github.com/gloathub/gojava) port grows.

Files are `.clj` (not `.glj`) so they go through the rewrite step that
translates JVM-style `Math/*`, `System/*`, `Integer/*`, `Long/*`,
`String/*`, `Double/*`, `Boolean/*`, and `Character/*` symbols into calls
on the glojure-internal javacompat bridge. String instance methods like
`(.toUpperCase s)` dispatch at runtime through a string-method registry.
The bridge then forwards to gojava's typed Go port of `java.lang.*`,
returning JVM-faithful results.

Run one with:

```bash
gloat --run 01-static-method.clj
```

Or run them all in order:

```bash
for f in [0-9][0-9]-*.clj; do
  echo "=== $f ==="
  gloat --run "$f"
done
```

| File | Topic |
|------|-------|
| `01-static-method.clj`          | Calling static methods (`Math/sqrt`, `Math/pow`) |
| `02-static-field.clj`           | Static field/constants (`Math/PI`, `Math/E`) |
| `03-polymorphic-dispatch.clj`   | JVM-style overloading by argument type |
| `04-jvm-semantics.clj`          | Where JVM behaviour diverges from Go's stdlib |
| `05-dual-interop.clj`           | JVM-style `Math/sqrt` and Go-style `math.Sqrt` together |
| `06-system.clj`                 | `java.lang.System`: time, env, properties, streams |
| `07-integer-long.clj`           | `java.lang.Integer` and `java.lang.Long`: parsing, constants, bit ops |
| `08-string.clj`                 | `java.lang.String`: instance methods, statics, JVM semantics |
| `09-double.clj`                 | `java.lang.Double`: parsing, predicates, JVM-style `toString` |
| `10-boolean.clj`                | `java.lang.Boolean`: parsing, lenient `valueOf`, logical ops |
| `11-character.clj`              | `java.lang.Character`: predicates, case folding, radix digits |
