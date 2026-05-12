Go Interop Examples
===================

Short, runnable Glojure programs that demonstrate each section of the
[`gloat-go-interop`](../../../doc/gloat-go-interop.md) page.

Each file is self-contained.
Run one with:

```bash
gloat --run 01-hello-package-fn.glj
```

Or run them all in order:

```bash
for f in [0-9][0-9]-*.glj; do
  echo "=== $f ==="
  gloat --run "$f"
done
```

| File | Topic |
|------|-------|
| `01-hello-package-fn.glj` | Calling Go package functions (`fmt`, `strings`) |
| `02-fn-into-go-hof.glj`   | Passing a Glojure `fn` to a Go higher-order function |
| `03-method-call.glj`      | Method calls -- both `.` forms and a zero-arg method |
| `04-fields-and-set.glj`   | Reading and writing struct fields with `set!` |
| `05-new-and-init.glj`     | Constructing a struct with `new`, then initialising fields |
| `06-handler-func-wrap.glj`| Wrapping a Glojure fn as a Go function value |
| `07-multi-return.glj`     | Destructuring Go's `(value, error)` returns |
| `08-slice-of-byte.glj`    | `go/slice-of` and other collection type constructors |
| `09-reflect-escape.glj`   | Dropping to `reflect` when higher-level forms don't cover it |
