ULID Deps Demo
==============

A tiny program that calls into a third-party Go module
([`github.com/oklog/ulid/v2`](https://github.com/oklog/ulid)) via Gloat,
demonstrating how `gljdeps.edn` declares Go module dependencies for
AOT compilation.

`oklog/ulid` was chosen specifically because it is *not* already a
dependency of Glojure itself, so the demo exercises the real deps
resolution path rather than coincidentally finding a bundled module.

## Running

```bash
gloat --run ulid.glj          # AOT compile and run
gloat ulid.glj -o ulid        # produce a standalone binary
./ulid
```

All three resolution mechanisms for `gljdeps.edn` work here:

```bash
gloat --run ulid.glj                                    # auto-detects ./gljdeps.edn
gloat --deps=./gljdeps.edn --run ulid.glj               # explicit path
GLOAT_GLJDEPS=./gljdeps.edn gloat --run ulid.glj        # via env var
```

## What This Demonstrates

`gljdeps.edn` declares Go module dependencies that Glojure code can call
into. The EDN shape is:

```clojure
{:deps {github.com:oklog:ulid:v2 {:mvn/version "v2.1.0"}}}
```

The symbol key is a Go module path with `:` substituted for `/`, matching
the syntax Glojure uses when calling into Go packages
(see [`doc/gloat-go-interop.md`](../../../doc/gloat-go-interop.md)).
The value is a Clojure deps-style map naming the module version.

When Gloat AOT-compiles `ulid.glj`, it:

1. Resolves `gljdeps.edn` (from `--deps=`, `GLOAT_GLJDEPS`, or the CWD).
2. Copies it into the temporary glj workspace so `glj compile` sees it.
3. Injects matching `require` lines into the generated `go.mod` so the
   final `go build` pulls the module from the Go module cache.

The same `gljdeps.edn` drives the REPL: from this directory you can run
`gloat --repl` and call `(github.com:oklog:ulid:v2.Make)` interactively
without changing or duplicating the dep declaration.

## The Program

`ulid.glj` exercises three different interop shapes against the ulid
module to confirm the dep is wired up end-to-end:

- Package function: `(github.com:oklog:ulid:v2.Make)`
- Method on a returned value: `(.String id)`
- Multi-return tuple: `[parsed err] (github.com:oklog:ulid:v2.Parse s)`

Each cross-package call here would fail at compile time without
`gljdeps.edn`, because the generated Go code references
`github.com/oklog/ulid/v2` as an import path that has to be in `go.mod`.

## A Note on `v2`

The trailing `/v2` in the module path is Go's module versioning
convention; it indicates "version 2 of this module." Inside the
package, the Go identifier is still `ulid` (not `v2`). Glojure's
colonified-path syntax follows the full import path
(`github.com:oklog:ulid:v2`) and resolves to the package's actual name
internally, so `(github.com:oklog:ulid:v2.Make)` works correctly.
