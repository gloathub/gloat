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
gloat --run ulid.clj          # AOT compile and run
gloat ulid.clj -o ulid        # produce a standalone binary
./ulid
```

All three resolution mechanisms for `gljdeps.edn` work here:

```bash
gloat --run ulid.clj                                    # auto-detects ./gljdeps.edn
gloat --deps=./gljdeps.edn --run ulid.clj               # explicit path
GLOAT_GLJDEPS=./gljdeps.edn gloat --run ulid.clj        # via env var
```

## What This Demonstrates

`gljdeps.edn` declares Go module dependencies that Glojure code can call
into. The EDN shape is:

```clojure
{:deps {github.com:oklog:ulid:v2 {:mvn/version "v2.1.0"}}}
```

The symbol key is a Go module path with `:` substituted for `/`, matching
the syntax Glojure uses when calling into Go packages
(see [`doc/gloat-go-interop.md`](../../../../doc/gloat-go-interop.md)).
The value is a Clojure deps-style map naming the module version.

When Gloat AOT-compiles `ulid.clj`, it:

1. Resolves `gljdeps.edn` (from `--deps=`, `GLOAT_GLJDEPS`, or the CWD).
2. Copies it into the temporary glj workspace so `glj compile` sees it.
3. Injects matching `require` lines into the generated `go.mod` so the
   final `go build` pulls the module from the Go module cache.

The same `gljdeps.edn` drives the REPL: from this directory you can run
`gloat --repl` and call `(github.com:oklog:ulid:v2.Make)` interactively
without changing or duplicating the dep declaration.

## The Program

`ulid.clj` exercises three different interop shapes against the ulid
module to confirm the dep is wired up end-to-end:

- Package function: `(github.com:oklog:ulid:v2.Make)`
- Method on a returned value: `(.String id)`
- Multi-return tuple: `[parsed err] (github.com:oklog:ulid:v2.Parse s)`

## What `gljdeps.edn` Actually Buys You

Gloat's AOT pipeline runs `go mod tidy` before `go build`, and Go's
own tooling will resolve missing imports against the public module
proxy on demand. That means the demo will *also* build successfully
with `gljdeps.edn` renamed or absent: Go discovers
`github.com/oklog/ulid/v2` via the proxy, picks the latest version,
and adds it to the generated `go.mod` automatically.

So `gljdeps.edn` is not strictly required for an AOT build to
succeed when the public proxy is reachable. Where it *is* required
or load-bearing:

1. **Version pinning.** Without the deps file you get whatever is
   currently latest on the proxy. With it, you get the version you
   declared. Pinning matters for reproducible builds.
2. **REPL mode (`gloat --repl`).** There is no `go build` step at the
   end of the REPL flow, so `go mod tidy` never runs to discover the
   dep. The deps file is genuinely required here.
3. **Private modules.** The public proxy cannot resolve them; an
   explicit declaration plus `GOPRIVATE` is needed.
4. **Offline or restricted builds (`GOPROXY=off`).** Auto-discovery
   only works when the proxy is reachable. Pinned versions can come
   from the local module cache without a network round-trip.

Try it both ways from this directory:

```bash
gloat --run ulid.clj                  # uses ./gljdeps.edn -> v2.1.0
mv gljdeps.edn gljdeps.edn.bak
gloat --run ulid.clj                  # works via auto-discovery -> v2.1.x latest
mv gljdeps.edn.bak gljdeps.edn
```

The second run produces a `go: finding module for package ...` line
from `go mod tidy`, which is the signature of the proxy fallback.

## A Note on `v2`

The trailing `/v2` in the module path is Go's module versioning
convention; it indicates "version 2 of this module." Inside the
package, the Go identifier is still `ulid` (not `v2`). Glojure's
colonified-path syntax follows the full import path
(`github.com:oklog:ulid:v2`) and resolves to the package's actual name
internally, so `(github.com:oklog:ulid:v2.Make)` works correctly.
