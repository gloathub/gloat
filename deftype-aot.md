# AOT `deftype` / `reify` for Glojure

A plan for adding AOT-only `deftype` (and a narrow `reify`) to Glojure so
that Glojure code can implement Go interfaces directly, eliminating the
need for hand-written Go shims like `demo/interop/bubbletea/shim.go`.

## Verification: the current state

Claim under test: "Glojure does not support `deftype` or `reify`, so there
is no way to implement a Go interface directly from Clojure code."

Verdict: **accurate**. Trail through `/home/ingy/src/glojure`:

1. **Reader recognizes the symbols only.** `pkg/reader/reader.go:42-43`
   lists `"deftype*"` and `"reify*"` as known reader symbols. Recognition
   is not implementation.

2. **Only `deftype*` is in the Specials set; `reify*` is absent.**
   `pkg/runtime/evalast.go:230-253`:
   ```go
   func (c *evalCompiler) Specials() *lang.Set {
       return lang.NewSet(
           ...
           lang.NewSymbol("case*"),
           lang.NewSymbol("deftype*"),
       )
   }
   ```

3. **No AST node type exists for either form.** `pkg/ast/ast.go:216-250`
   defines every `NodeOp` constant (`OpConst`, `OpDef`, `OpFn`, ...).
   There is no `OpDeftype` and no `OpReify`. The analyzer cannot produce
   an AST node for these forms.

4. **No evaluator handler.** `EvalAST` switch in
   `pkg/runtime/evalast.go:73-136` covers ~30 ops; the `default` branch
   panics with `"unimplemented op"`.

5. **No codegen handler.** `pkg/runtime/codegen.go:1125-1184` (the AST to
   Go emitter) has no deftype/reify case.

6. **No Clojure-level macros.** `pkg/stdlib/clojure/core_deftype.glj`
   only defines `defprotocol`, `extend`, `extend-type`,
   `extend-protocol`, and helpers. No `deftype`, no `reify`, no
   `defrecord`. The file is 251 lines, a heavy reduction of upstream
   Clojure's `core_deftype.clj`.

7. **Glojure documents this itself** in `core_deftype.glj:22-25`:
   > "Go's reflection capabilities don't yet support a native
   > interface-based implementation, so protocols are implemented in
   > Glojure as maps from type to protocol method implementations."

8. **No runtime escape hatch in `pkg/lang/builtins.go`.** `GoMake` only
   handles slices, maps, and channels; `GoNew` only does
   `reflect.New(type)` (zero struct). There is no
   `reflect.MakeInterface(iface, methodMap)` in Go, so a
   runtime-constructed interface implementation is structurally
   impossible.

## Why this is a hard limit at runtime, and not at compile time

**Runtime (interpreted) path is blocked.** Go's `reflect` package can
synthesize a function value (`reflect.MakeFunc`) but cannot synthesize
a method set on a new type. Without a `reflect.MakeInterface(...)`,
there is no way to produce a value satisfying `tea.Model` (or any
other Go interface) from inside the running glj REPL. This is a Go
language limitation, not a Glojure limitation, and there is no clean
workaround.

**AOT (codegen) path is open.** gloat already turns `.glj` into `.go`
source via `glj compile`. A `deftype` form can be emitted as a real
Go `type Foo struct { ... }` with real methods. At Go build time it
satisfies any interface its method set matches, exactly as if a human
had written it. The Glojure compiler just needs to know:
  - the field names and types (struct shape)
  - the methods, with their receiver and signatures
  - which Go interface(s), if any, the type is meant to satisfy (so
    the emitted Go method signatures match the interface)

## Approach

### Scope decision: AOT-only

Implement `deftype` and `reify` purely in the AOT compilation path. The
glj REPL continues to reject them. This matches the asymmetry already
present in gloat: AOT code can call third-party Go packages, the REPL
needs `gljdeps.edn` and a build step.

This means:
  - At the REPL, `(deftype Foo [a])` still produces an error.
  - In `gloat foo.clj` (or `--run`, or `-o build/`), `(deftype Foo [a])`
    emits a real Go struct in the output package.

### Where the Go interface contract comes from

`deftype` in standard Clojure mixes protocol implementations and Java
interface implementations. In gloat the analog is: protocol
implementations stay as the existing map-based dispatch; Go interface
implementations need to name the interface so we can emit signatures
that match.

Reuse the colonified Go path syntax that gloat already supports for
calls and imports:

```clojure
(deftype Picker [state]
  github.com:charmbracelet:bubbletea.Model
  (Init [this] nil)
  (Update [this msg]
    ...returns [this nil]...)
  (View [this]
    (picker.core/view-model state)))
```

The compiler resolves `github.com:charmbracelet:bubbletea.Model` to a
Go interface (it already resolves the same syntax for function calls),
reads its method set, and emits matching Go methods.

### Pipeline changes

1. **AST node.** Add `OpDeftype` and `OpReify` to `pkg/ast/ast.go`.
   Add `DeftypeNode` and `ReifyNode` structs with fields for the type
   name, the field list, the interface symbols, and the method
   implementations (each method is itself a closure-like AST tree).

2. **Analyzer / parser.** The analyzer (Clojure side, in glojure's
   compiled stdlib) needs `parse-deftype*` and `parse-reify*` to
   construct the new AST nodes. Today these forms never get past the
   reader because the analyzer has no parse rule for them.

3. **Macros.** Add to `core_deftype.glj`:
   - `deftype` macro that lowers to `deftype*`
   - `reify` macro that lowers to `reify*` (or directly to a `deftype*`
     of a generated gensym name plus a constructor invocation).
   The macros do the same kind of spec parsing the upstream Clojure
   versions do, but the emitted special form is the new
   gloat-compatible one, not the JVM one.

4. **Codegen.** In `pkg/runtime/codegen.go`, add cases for `OpDeftype`
   and `OpReify`. Each emits:
   - a top-level Go `type Foo struct { ... }` matching the field list,
   - one Go method per method in the form, with the receiver typed
     `*Foo` (or `Foo`, see below) and parameter/return types lifted
     from the named Go interface,
   - a constructor `func NewFoo(...) *Foo` so the form
     `(Foo. a b c)` (Clojure constructor sugar) keeps working,
   - for `reify`, the `type` declaration uses a gensym name and the
     reifying expression evaluates to `&_Reify123{captured fields...}`.

5. **Receiver type.** Clojure's `deftype` produces immutable
   value-like types with non-mutable fields. Go interface
   satisfaction is by method set, which differs between `T` and `*T`.
   For interop, default to `*T` receivers (matches how almost every Go
   library expects implementations) and revisit if a real case wants
   value receivers.

6. **Body lowering.** Method bodies are Clojure expressions and need
   the existing codegen path. The new node types just frame the body
   inside a Go function literal whose signature matches the interface
   method; the body's last expression becomes the Go return value(s).

   Multi-return Go signatures (e.g.,
   `Update(tea.Msg) (tea.Model, tea.Cmd)`) need the body's tail to be
   a Clojure vector of two values, destructured into Go's tuple
   return. This is the only nontrivial signature shape in the
   bubbletea use case and is a clean codegen rule.

7. **Field access.** Inside method bodies, the bound parameter (e.g.,
   `this`) needs to expose field reads via the existing host interop
   (`(.state this)`), which already works for Go structs in gloat.
   No new mechanism needed.

8. **REPL guard.** In `pkg/runtime/evalast.go`, when the new ops reach
   `EvalAST` (i.e., we are not in the AOT codegen path), return a
   clear error: `"deftype/reify are only supported in AOT compilation,
   not at the REPL"`. Better than the current "unimplemented op" panic.

### Worked example: replacing `shim.go`

Currently `demo/interop/bubbletea/shim.go` is ~90 lines of Go that
implements `tea.Model` and delegates to four Clojure functions. With
this feature, `picker.clj` would gain:

```clojure
(deftype Picker [state]
  github.com:charmbracelet:bubbletea.Model
  (Init [this] nil)
  (Update [this msg]
    (if (instance? github.com:charmbracelet:bubbletea.KeyMsg msg)
      (let [next (update-model state (.String msg))]
        (cond
          (nil? next)             [this github.com:charmbracelet:bubbletea.Quit]
          (format-result next)    [(Picker. next) github.com:charmbracelet:bubbletea.Quit]
          :else                   [(Picker. next) nil]))
      [this nil]))
  (View [this]
    (view-model state)))

(defn -main []
  (let [p (github.com:charmbracelet:bubbletea.NewProgram (Picker. (init-model)))]
    (let [[final err] (.Run p)]
      (when err
        (println "Error:" err)
        (github.com:os.Exit 1))
      (when-let [s (format-result (.state final))]
        (println s)))))
```

`shim.go` deletes. The Makefile drops the `cp shim.go build/main.go`
step. Everything is in Clojure.

### Critical files (in glojure)

- `pkg/ast/ast.go` add `OpDeftype`, `OpReify`, `DeftypeNode`,
  `ReifyNode`
- `pkg/runtime/evalast.go` add Specials entry for `reify*`; reject
  both at runtime with a clear error
- `pkg/runtime/codegen.go` add codegen cases that emit Go `type` and
  methods; add tuple-return rule for multi-value method signatures
- `pkg/stdlib/clojure/core_deftype.glj` add `deftype` and `reify`
  macros; keep the existing protocol machinery
- Analyzer (Clojure side, wherever `parse-*` for special forms lives;
  this is the rewrite layer between `.clj` and the AST) add
  `parse-deftype*` and `parse-reify*`

### Critical files (in gloat)

None for the core feature, beyond:

- `doc/gloat-go-interop.md` document the new form and the
  AOT-only restriction
- `demo/interop/bubbletea/` rewrite as the worked example above,
  drop `shim.go`, simplify `Makefile`

### Verification checklist

1. `(deftype Foo [a b])` compiles and `(Foo. 1 2)` constructs an
   instance whose `(.a x)` returns `1`.
2. `(deftype Foo [a] some.Interface (Method [this] ...))` produces a
   Go file that compiles and where `var _ some.Interface = (*Foo)(nil)`
   succeeds.
3. Multi-return Go interface method (`Update`) works when the body
   returns a Clojure vector of the right arity.
4. `reify` over a Go interface, used as a let-expression value,
   produces a working anonymous implementation. Field capture works
   for outer let-bound names.
5. At the REPL, `(deftype Foo [a])` returns the new error, not a
   panic.
6. The bubbletea demo, rewritten to use `deftype`, builds and runs
   identically to the current shim-based version.
7. `make test` is green; no regression in `defprotocol` /
   `extend-type` / `extend-protocol`.

### Out of scope (intentionally)

- `defrecord`. Could follow the same recipe but adds map-equality
  semantics. Not needed for Go interop. Park.
- Runtime `reify` at the REPL. Blocked by `reflect`. Not fixable
  without code generation.
- Mutable fields (`^:volatile-mutable`, `^:unsynchronized-mutable`).
  Skip until a real case asks.
- Method overloading by arity. Go doesn't support it; reject at the
  analyzer.

## Why this is worth doing

Every Go library that exposes an interface as its primary API
(`io.Reader`, `tea.Model`, `http.Handler`, `sql.Driver`, ...) currently
requires a Go shim to use from gloat. That is the friction point that
shapes the bubbletea demo's whole architecture and dominates its
ReadMe. Closing this gap turns gloat from "Clojure that can call Go
functions" into "Clojure that can implement Go contracts," which is
where the interop story has to go for serious applications.
