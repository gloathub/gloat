# Gloat Ecosystem Tutorial

Gloat compiles YAMLScript, Clojure, or Glojure to Go source, native binaries,
WebAssembly, and shared libraries.

## Compilation Pipeline

The normal YAMLScript pipeline is:

```text
.ys -> ys -T clj -> .clj -> Glojure rewrite -> .glj
    -> glj compile -> .go -> go build -> binary
```

The YAMLScript compiler produces portable Clojure that requires `ys.v0` and
calls `ys.v0/init`. Gloat uses the source tree shipped by `ys-v0-go` while
Glojure analyzes the application, then links the precompiled runtime loaders
from that module into the generated Go program.

## Repositories

The main repositories in the build are:

- `gloathub/gloat` — the compiler orchestration and output templates.
- `gloathub/ys-v0-go` — patched portable `ys.v0` sources, Glojure-native
  backends, and generated Go loaders.
- `glojurelang/glojure` — the Clojure-to-Go compiler and runtime.
- `yaml/yamlscript` — the YAMLScript compiler and published `ys.v0` sources.
- `makeplus/makes` — repository-local tool and dependency management.

The runtime has its own release lifecycle. Gloat pins it with
`YS-V0-GO-VERSION` in `common/common.mk`; a Gloat release does not publish or
tag the runtime module.

## Repository Layout

Important Gloat paths are:

```text
gloat/
|-- bin/gloat             Bash CLI entry point
|-- src/gloat.clj         Compiler orchestration
|-- src/prune.clj         Dependency graph and pruning
|-- template/             Generated Go module templates
|-- common/common.mk      Pinned dependency versions
|-- util/make-do          Build and release helpers
|-- ys/lg/                let-go runtime sources
`-- repos/ys-v0-go/       Optional development checkout
```

Gloat no longer keeps copies of the YAMLScript Clojure, Glojure, or generated
Go runtime trees. Installed copies clone the pinned `ys-v0-go` tag into
`.cache/ys-v0-go`; a checkout at `repos/ys-v0-go` takes precedence for local
development.

The standalone runtime repository contains:

```text
ys-v0-go/
|-- source/               Exact patched analysis sources
|-- ys/                   Generated ys namespace loaders
|-- yamlscript/           Generated compatibility loaders
|-- babashka/             Go-native process and HTTP backends
|-- clojure/              Go-native JSON and walk loaders
|-- runtime/              Ordered Load function and manifests
|-- bb/runtime.clj        Self-contained Babashka runtime
|-- src/                   Hand-maintained Go backend sources
|-- patches/               Temporary upstream AOT patches
`-- script/generate       Reproducible source and loader generation
```

## Generated Go Modules

For a directory, binary, library, or WebAssembly output, Gloat:

1. Compiles YAMLScript to portable Clojure.
2. Copies `ys-v0-go/source` into a temporary compiler workspace.
3. Rewrites and compiles the application namespace with Glojure.
4. Copies only application loaders into the generated module.
5. Renders `go.mod` and the appropriate Go entry point.
6. Links `github.com/gloathub/ys-v0-go/runtime` and calls `runtime.Load()`
   before requiring the application namespace.

The generated `go.mod` has this dependency shape:

```go
require (
    github.com/glojurelang/glojure GLOJURE-VERSION
    github.com/gloathub/ys-v0-go YS-V0-GO-VERSION
)
```

During development Gloat adds a local `replace` pointing at
`YS-V0-GO-DIR`. Published generated modules must omit local `replace`
directives so the Go proxy can resolve the tagged module.

## Runtime Loading and Pruning

The normal templates blank-import the generated application package and call
`ys-v0-go/runtime.Load()`. The loader registers and requires the runtime
namespaces in the order recorded by `runtime/namespaces.edn`.

With pruning enabled, Gloat analyzes references from the user loaders and the
runtime loaders in `ys-v0-go`. It copies the retained runtime packages into
the generated module's internal tree and emits ordered imports and requires.
This keeps pruned programs independent of internal Gloat runtime copies while
allowing unused loader blocks to be removed.

## Source Formats

Gloat can also stop before building Go:

- `-t clj` emits the portable Clojure produced by YAMLScript.
- `-t bb` prepends `ys-v0-go/bb/runtime.clj`, producing a self-contained
  Babashka program with no run-time Java or Maven requirement.
- `-t glj` emits rewritten Glojure source.
- `-t go` emits the generated application loader.
- `-t lg` uses the `ys/lg` source tree for the let-go engine.

## Makes Bootstrap

Gloat and downstream projects use Makes to install pinned tools under
`.cache/local/`. The usual bootstrap is:

```makefile
M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
include $M/init.mk
```

Gloat includes modules for Babashka, Glojure, Go, and YAMLScript. Run
`make path` to install the declared tools and print an environment with their
paths.

## Development

Run the normal suite with:

```bash
make test
make test v=1
make test slow=1
```

To work on the runtime module beside Gloat:

```bash
git clone https://github.com/gloathub/ys-v0-go repos/ys-v0-go
make -C repos/ys-v0-go generate
make -C repos/ys-v0-go test
make test
```

The runtime generator normally downloads the pinned
`org.yamlscript/ys.v0` Clojars artifact. To test unpublished YAMLScript
sources, point it at the portable source tree:

```bash
YS_V0_SOURCE=/path/to/yamlscript/core/src \
  make -C repos/ys-v0-go generate
```

Temporary AOT compatibility changes live in `ys-v0-go/patches` and are
applied with zero fuzz before the Go-native backend sources are overlaid.
After changing the runtime, run `make check-generated` in that repository and
commit the source changes and generated loaders together.

## Version Management

The dependency pins used in generated modules come from
`common/common.mk`:

```makefile
GLOJURE-VERSION := 0.7.11
YS-V0-GO-VERSION := v0.1.0
```

`common/gloat-vars.mk` exposes the resolved versions and local checkout paths
to `src/gloat.clj`. Gloat renders those values into generated `go.mod` files.

Release the runtime module first when its code or generated loaders change.
After its tag is visible on the Go proxy, update `YS-V0-GO-VERSION`, validate
Gloat, and release Gloat separately.

## Downstream Projects

Downstream Makes projects typically include `gloat.mk`, generate a committed
Go submodule, and tag that submodule independently. A generated directory can
also be built directly:

```bash
gloat program.ys -o build/
make -C build
```

For local integration, keep the `replace` generated by Gloat. Before
publishing a downstream Go module, remove local replacements and verify that
both Glojure and `ys-v0-go` resolve from the Go proxy.
