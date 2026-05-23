Java Interop Examples
=====================

Short, runnable Clojure programs that demonstrate calling `java.lang.*`
from Glojure. Today the supported surface is `java.lang.Math`; the
pattern generalises to other classes as the
[gojava](https://github.com/gloathub/gojava) port grows.

Files are `.clj` (not `.glj`) so they go through the rewrite step that
translates JVM-style `Math/*` symbols into calls on the glojure-internal
javacompat bridge. The bridge then forwards to gojava's typed Go port of
`java.lang.Math`, returning JVM-faithful results.

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
