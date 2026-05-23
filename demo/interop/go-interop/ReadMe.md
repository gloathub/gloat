Go Interop Examples
===================

Short, runnable Glojure programs that demonstrate each section of the
[`gloat-go-interop`](../../../doc/gloat-go-interop.md) page.

Each file is self-contained.
Run one with:

```bash
gloat --run 01-hello-package-fn.clj
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
| `01-hello-package-fn.clj` | Calling Go package functions (`fmt`, `strings`) |
| `02-fn-into-go-hof.clj`   | Passing a Glojure `fn` to a Go higher-order function |
| `03-method-call.clj`      | Method calls -- both `.` forms and a zero-arg method |
| `04-fields-and-set.clj`   | Reading and writing struct fields with `set!` |
| `05-new-and-init.clj`     | Constructing a struct with `new`, then initialising fields |
| `06-handler-func-wrap.clj`| Wrapping a Glojure fn as a Go function value |
| `07-multi-return.clj`     | Destructuring Go's `(value, error)` returns |
| `08-slice-of-byte.clj`    | `go/slice-of` and other collection type constructors |
| `09-reflect-escape.clj`   | Dropping to `reflect` when higher-level forms don't cover it |
