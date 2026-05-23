Go Module Dependency Demos
==========================

Glojure code can call into any Go module, not just packages bundled with
Glojure itself.
These demos show how to declare those third-party dependencies so both
the AOT pipeline and the REPL can resolve them.

| Directory | What it shows |
|-----------|---------------|
| [`gljdeps-edn/`](gljdeps-edn/ReadMe.md) | Declaring a Go module dep via `gljdeps.edn`, picked up by `--run`, `-o`, and `--repl` |

## Background

Gloat resolves Go module deps from three sources, in order:

1. `--deps=path` on the command line
2. `GLOAT_GLJDEPS` environment variable
3. `./gljdeps.edn` in the current working directory

The file is plain edn:

```clojure
{:deps {github.com:oklog:ulid:v2 {:mvn/version "v2.1.0"}}}
```

Symbol keys use `:` where the Go module path uses `/`, matching the
syntax Glojure uses when calling into Go packages.

See [`doc/gloat-go-interop.md`](../../../doc/gloat-go-interop.md) for the
full interop reference.
