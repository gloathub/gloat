UUID Deps Demo
==============

A tiny program that calls into a third-party Go module
([`github.com/google/uuid`](https://github.com/google/uuid)) via Gloat,
demonstrating how `gljdeps.edn` declares Go module dependencies for
AOT compilation.

## Running

```bash
gloat --run uuid.glj          # AOT compile and run
gloat uuid.glj -o uuid        # produce a standalone binary
./uuid
```

All three resolution mechanisms for `gljdeps.edn` work here:

```bash
gloat --run uuid.glj                                    # auto-detects ./gljdeps.edn
gloat --deps=./gljdeps.edn --run uuid.glj               # explicit path
GLOAT_GLJDEPS=./gljdeps.edn gloat --run uuid.glj        # via env var
```

## What This Demonstrates

`gljdeps.edn` declares Go module dependencies that Glojure code can call
into. The EDN shape is:

```clojure
{:deps {github.com:google:uuid {:mvn/version "v1.6.0"}}}
```

The symbol key is a Go module path with `:` substituted for `/`, matching
the syntax Glojure uses when calling into Go packages
(see [`doc/gloat-go-interop.md`](../../../doc/gloat-go-interop.md)).
The value is a Clojure deps-style map naming the module version.

When Gloat AOT-compiles `uuid.glj`, it:

1. Resolves `gljdeps.edn` (from `--deps=`, `GLOAT_GLJDEPS`, or the CWD).
2. Copies it into the temporary glj workspace so `glj compile` sees it.
3. Injects matching `require` lines into the generated `go.mod` so the
   final `go build` pulls the module from the Go module cache.

The same `gljdeps.edn` drives the REPL: from this directory you can run
`gloat --repl` and call `(github.com:google:uuid.New)` interactively
without changing or duplicating the dep declaration.

## The Program

`uuid.glj` exercises three different interop shapes against the uuid
module to confirm the dep is wired up end-to-end:

- Package function: `(github.com:google:uuid.New)`
- Method on a returned value: `(.String id)`
- Multi-return tuple: `[parsed err] (github.com:google:uuid.Parse s)`

Each cross-package call here would fail at compile time without
`gljdeps.edn`, because the generated Go code references
`github.com/google/uuid` as an import path that has to be in `go.mod`.
