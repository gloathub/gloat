# gloat-go-interop -- Calling Go from Glojure

## Synopsis

Gloat compiles Glojure (a Clojure dialect) to Go.
Every Go package that the compiler has been linked against is therefore
visible from Glojure code, and every value flows end-to-end as its
natural Go type.
The standard library is always available.
Third-party Go packages become available when they are declared in a
`gljdeps.edn` file (see [gloat-repl](gloat-repl.md) for using deps from
the REPL).

The interop surface is small and looks a lot like Clojure's Java interop.
Package functions are called as if they were Glojure functions
(`fmt.Sprintf`, `strings.Split`).
Methods, struct fields, and constructors use the dot (`.`), `set!`, and
`new` forms.
A reflection escape hatch is available for anything the higher-level
forms cannot reach.

The following short program touches a package function, a method, a
field, and a constructor.
Everything else in this page is variations on these four moves.

```clojure
(ns main.core)

(defn -main []
  (println (fmt.Sprintf "Hello, %s!" "world"))
  (println (strings.HasPrefix "foobar" "foo"))
  (let [parts (strings.Split "alpha,beta,gamma" ",")]
    (println (strings.Join parts " | "))))
```

To experiment with interop interactively, start the REPL with
`gloat --repl`; see [gloat-repl](gloat-repl.md) for how to load
third-party Go packages via `gljdeps.edn`.


## Calling Go Package Functions

Runnable: [`01-hello-package-fn.clj`](../demo/interop/go-interop/01-hello-package-fn.clj)

A Go package function is called with the form `(pkg.Function args...)`.
The compiler resolves the identifier directly to the package's exported
function, so there is no `import` form to write and no aliasing step.
Glojure values cross the boundary as their natural Go counterparts:
strings as `string`, longs as `int64`, vectors flow into slice
parameters, and so on.

```clojure
(ns main.core)

(defn -main []
  (println (fmt.Sprintf "Hello, %s!" "world"))
  (println (strings.HasPrefix "foobar" "foo"))
  (let [parts (strings.Split "alpha,beta,gamma" ",")]
    (println (strings.Join parts " | "))))
```

The package portion of the name is a Go import path.
For short paths (`fmt`, `strings`, `bytes`, `regexp`) you write them
verbatim.
For paths that contain a `/`, replace the slashes with `:` in Glojure
code: `net:http.Server`, `net:url.Parse`, `crypto:sha256.Sum256`.
Fully-qualified module paths use the same separator throughout.

```clojure
(throw (github.com:gloathub:glojure:pkg:lang.NewError "boom"))
```

A Glojure function can be passed wherever Go expects a function value.
The compiler generates a Go closure that calls back into the Glojure
runtime, so higher-order Go APIs work the same as Clojure ones.

Runnable: [`02-fn-into-go-hof.clj`](../demo/interop/go-interop/02-fn-into-go-hof.clj)

```clojure
(ns main.core)

(defn -main []
  (let [parts (strings.FieldsFunc
                "alpha,beta;gamma|delta"
                (fn [c] (contains? #{\, \; \|} (char c))))]
    (doseq [p parts]
      (println " -" p))))
```


## Packages Are Linked, Not Imported

Glojure has no per-file form for pulling in a Go package.
There is no `(:import ...)` clause for Go types, no `(:require ... :as
alias)` for Go packages, and no way to give a Go package a local short
name that applies only to the current file.
A Go package is either linked into the binary (in which case every name
inside it is callable from anywhere) or it is not.

What you *do* still write is the standard Clojure `ns` form for Glojure
namespaces:

```clojure
(ns main.core
  (:require [clojure.string :as str]))
```

That `:require` only affects Glojure namespaces.
Go packages are addressed directly by their import path everywhere they
are used, with `/` rewritten as `:` (see **Calling Go Package
Functions** above).

The Go standard library is always linked.
Third-party Go packages are declared in a `gljdeps.edn` file that sits
next to the program being run; the compiler reads it, fetches the
modules with `go get`, and links the listed packages into the resulting
binary.

A minimal `gljdeps.edn`:

```clojure
{:deps {github.com:google:uuid {:mvn/version "v1.6.0"}}}
```

The key is a Glojure symbol naming the Go import path, written in the
same colon-separated form used at call sites.
The deps loader rewrites `:` back to `/` before handing the path to
`go get`, so this entry resolves to the module `github.com/google/uuid`.
The slash form (`github.com/google/uuid`) is also accepted at the
deps-key level, but at call sites you must use the colon form because
the dot in a multi-slash slash form would parse as a host expression.
Stick with the colon form everywhere for consistency.

A program that uses it (saved next to the deps file):

```clojure
(ns main.core)

(defn -main []
  (let [id (github.com:google:uuid.New)]
    (println "Generated UUID:" (.String id))))
```

Running it:

```
$ gloat --run greet.glj
Generated UUID: 69e566b7-f49c-4302-a11f-808ebcd96112
```

All compile modes (including `gloat --run` and binary builds) and the
REPL look for `gljdeps.edn` the same way: an explicit `--deps=path`
takes precedence, then `GLOAT_GLJDEPS` from the environment, then a
`gljdeps.edn` in the current working directory.
See [gloat-repl](gloat-repl.md) for REPL specifics.
First launch fetches each module; subsequent launches reuse the cache.

If a fully-qualified path is unwieldy at the call site, give it a
local short name with `def`:

```clojure
(def uuid-new github.com:google:uuid.New)

(defn -main []
  (println "Generated UUID:" (.String (uuid-new))))
```

This is a plain Glojure binding, not a package alias; it lives in the
current namespace and does not affect resolution anywhere else.


## Methods on Values

Runnable: [`03-method-call.clj`](../demo/interop/go-interop/03-method-call.clj)

Method calls use the dot form, identical to Clojure's Java interop.
Both shapes are supported and produce the same code:

```
(. instance Method args...)
(. instance (Method args...))
```

The grouped form (parens around the method call) is easier to read in
nested or chained expressions; the flat form is shorter for one-shot
calls.
Pick whichever reads better in context.
Zero-argument methods take neither parens nor args.

```clojure
(ns main.core)

(defn -main []
  (let [escaper (strings.NewReplacer "<" "&lt;" ">" "&gt;")]
    (println (. escaper (Replace "<html>")))
    (println (. escaper Replace "the <header> tag")))
  (println "Len of empty Buffer:" (. (new bytes.Buffer) Len)))
```

Pointer and value receivers are not distinguished at the call site.
`new` returns `*T` (see below), and method lookup walks both the
pointer and value method sets, so calling either kind of method just
works.


## Reading and Writing Struct Fields

Runnable: [`04-fields-and-set.clj`](../demo/interop/go-interop/04-fields-and-set.clj)

The same dot form reads exported struct fields.
There is no separate `.-field` form; the compiler decides whether the
member is a field or a method by looking at the type.

```clojure
(. instance FieldName)
```

To assign a field, wrap the field-read in `set!`:

```clojure
(set! (. instance FieldName) value)
```

```clojure
(ns main.core)

(defn -main []
  (let [srv (new net:http.Server)]
    (println "Addr before:" (pr-str (. srv Addr)))
    (set! (. srv Addr) ":8080")
    (println "Addr after: " (pr-str (. srv Addr)))))
```

Field types must match.
Assigning an `int64` to a `time.Duration` field will not coerce; cast
first with `(time.Duration ...)` or build the value through the package
that owns the type.

One gotcha: if a field's type is itself a function, reading the field
with the dot form invokes it.
To inspect a function-typed field without calling it, drop to
`reflect` (see **Reflection Escape Hatch**).


## Constructing Values with `new`

Runnable: [`05-new-and-init.clj`](../demo/interop/go-interop/05-new-and-init.clj)

`new` returns a fresh, zero-valued pointer to a Go type.

```clojure
(new pkg.Type)        ;; => *pkg.Type, all fields zero
```

Initialise the struct by writing its fields with `set!`:

```clojure
(ns main.core)

(defn -main []
  (let [srv (new net:http.Server)]
    (set! (. srv Addr)           "localhost:8080")
    (set! (. srv MaxHeaderBytes) (int (* 1024 16)))
    (println "Addr:          " (. srv Addr))
    (println "MaxHeaderBytes:" (. srv MaxHeaderBytes))))
```

Where a struct field expects a function-typed interface value (the
canonical example is `net/http.Handler`), wrap a Glojure fn in the
package's `*Func` adapter
(see [`06-handler-func-wrap.clj`](../demo/interop/go-interop/06-handler-func-wrap.clj)):

```clojure
(ns main.core)

(defn handler [w r]
  (. w (WriteHeader 200)))

(defn -main []
  (let [srv (new net:http.Server)]
    (set! (. srv Addr)    "localhost:8080")
    (set! (. srv Handler) (net:http.HandlerFunc handler))
    (let [field (.FieldByName (.Elem (reflect.ValueOf srv)) "Handler")]
      (println "Addr:    " (. srv Addr))
      (println "Handler: " (if (.IsNil field) "nil" "<set>")))))
```

`net/http.HandlerFunc` is a Go type whose conversion `T(f)` adapts a
plain function into the `http.Handler` interface.
Calling it on a Glojure fn produces a value the field will accept.
Construction of non-pointer struct literals and direct assembly of
typed slices and maps from Glojure values are limited; see
**Known Limits**.


## Multiple Return Values

Runnable: [`07-multi-return.clj`](../demo/interop/go-interop/07-multi-return.clj)

Go functions that return more than one value come back to Glojure as a
vector.
Destructure the vector at the binding site to name each return:

```clojure
(ns main.core)

(defn -main []
  (let [[n   err]  (strconv.Atoi "42")
        [n2  err2] (strconv.Atoi "not-a-number")]
    (println "good input ->" n "  err:" err)
    (println "bad input  ->" n2 "  err:" err2)
    (when (some? err2)
      (println "  error message:" (.Error err2)))))
```

A `nil` error is the success case, matching Go convention.
Test for failure with `(some? err)` or by truthiness.
The error value is a Go `error`; call `(.Error err)` to get the
message string.

A common idiomatic shape is to throw on error and keep going:

```clojure
(let [[n err] (strconv.Atoi s)]
  (when err (throw err))
  n)
```


## Collections: Slices, Arrays, Maps

Runnable: [`08-slice-of-byte.clj`](../demo/interop/go-interop/08-slice-of-byte.clj)

Go types do not exist as Glojure literals.
To construct or cast to a Go collection type, use the type-builder
functions in the `go/` namespace.

| Builder | Result type |
|---------|-------------|
| `(go/slice-of T)` | `[]T` |
| `(go/array-of N T)` | `[N]T` |
| `(go/map-of K V)` | `map[K]V` |
| `(go/ptr-to T)` | `*T` |

Each builder returns a `reflect.Type`.
Applied like a function, it constructs or converts a value:

```clojure
(ns main.core)

(defn -main []
  (let [bs       ((go/slice-of go/byte) "foo----bar")
        [m err]  (regexp.Match "foo.*bar" bs)]
    (println "bytes count:" (count bs))
    (println "matched?    " m)
    (println "err:        " err)))
```

`go/byte`, `go/int`, `go/string`, `go/int64`, and so on are the
built-in type values you pass to a builder.

To allocate a fresh zero-value of any Go type, use `go/make`:

```clojure
(let [m (go/make (go/map-of go/string go/int))]
  ...)
```

Constructing a Go slice or map directly from a Glojure vector or map
literal is not yet supported through the type builders for every type.
Build incrementally, or use a Go helper.


## Reflection Escape Hatch

Runnable: [`09-reflect-escape.clj`](../demo/interop/go-interop/09-reflect-escape.clj)

A handful of Go features (function-typed field reads, channel
operations, dynamic field lookup, unexported behaviour, anything that
needs a `reflect.Type` at runtime) are not yet reachable through the
higher-level forms.
For those, drop to the `reflect` package directly.
The pattern is:

1. `reflect.ValueOf` to get a `reflect.Value` for any Go value.
2. `(.Elem v)` to dereference a pointer.
3. `(.FieldByName e "Name")`, `(.MethodByName e "Name")`, `(.Index e 0)`,
   `(.MapIndex e k)` to navigate.
4. `(.Interface f)` to escape back to a Glojure value, or `.Set` to
   write.

The example below reads three fields of an HTTP server by name,
including the function-typed `Handler` field that the dot form cannot
inspect:

```clojure
(ns main.core)

(defn -main []
  (let [srv  (new net:http.Server)
        _    (set! (. srv Addr) "localhost:8080")
        elem (.Elem (reflect.ValueOf srv))]
    (doseq [name ["Addr" "MaxHeaderBytes" "Handler"]]
      (let [f (.FieldByName elem name)]
        (println name "->" (.Interface f))))))
```

The same pattern works for channels (`.Send`, `.Recv`), tagged unions
hidden behind unexported types, and any other corner that does not yet
have a first-class Glojure form.
Treat it as a temporary bridge; high-traffic uses can usually be
factored into a small Go helper exposed as a normal package function.


## Known Limits

The forms above cover the common ground.
A few things look like they should work but do not, or do not yet:

- **`new` with keyword arguments** is recognised in the interpreter but
  silently ignored by the compiler.
  Write `(new pkg.Type)` followed by `set!` for each field.
- **Non-pointer struct construction** is not exposed.
  `new` always returns a pointer; use `(.Elem ...)` if you need the
  value form via reflection.
- **`defer` and `recover`** have no Glojure form yet.
  Use `try`/`finally` for the cleanup case; for `recover` semantics,
  catch the panic in a Go helper.
- **Address-of (`&`) and pointer dereference (`*`)** have no syntax of
  their own.
  `new` covers most pointer needs; `go/deref` (and reflection) covers
  the rest.
- **The blank identifier `_`** has no equivalent in destructuring.
  Bind a name you do not use, or pick one that documents the intent
  (`[v _err]`).
- **Implementing an arbitrary Go interface from Glojure** is supported
  only when the package ships a function-typed adapter (the
  `net/http.HandlerFunc` pattern).
  For other interfaces, write a small Go shim.
- **Building Go maps and slices from Glojure literals** through the
  type builders is partial.
  Use `go/make` and populate with reflection, or build the value in Go
  and return it.

These are gaps in the current Glojure compiler, not language
decisions; expect them to shrink over time.

Runnable copies of each example on this page live under
[`demo/interop/go-interop/`](../demo/interop/go-interop/ReadMe.md)
in the gloat repository.
Try them with `gloat --run path/to/file.clj`.
