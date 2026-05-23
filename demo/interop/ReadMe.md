Gloat Interop Demos
===================

Runnable examples that show how Glojure code calls into Go from Gloat.
Each subdirectory is self-contained, with its own ReadMe.

| Directory | What it shows |
|-----------|---------------|
| [`go-interop/`](go-interop/ReadMe.md)     | Numbered snippets keyed to [`doc/gloat-go-interop.md`](../../doc/gloat-go-interop.md): package calls, method calls, struct fields, `new`, multi-return, slices, `reflect` |
| [`java-interop/`](java-interop/ReadMe.md) | JVM-style `Math/*` interop via the javacompat bridge; keyed to [`doc/gloat-java-interop.md`](../../doc/gloat-java-interop.md) |
| [`bubbletea/`](bubbletea/ReadMe.md)       | Terminal UI built on [Bubbletea](https://github.com/charmbracelet/bubbletea); shows the hybrid Clojure-logic + Go-shim pattern when an interface needs implementing |
| [`deps/`](deps/ReadMe.md)                 | Declaring third-party Go module dependencies for Glojure code |

## Background

Gloat compiles Glojure (a Clojure dialect) to native Go binaries.
Glojure code calls Go functions using a colonified path syntax:
`(github.com:oklog:ulid:v2.Make)` is the Glojure form of
`ulid.Make()`.
The full reference is [`doc/gloat-go-interop.md`](../../doc/gloat-go-interop.md).

These demos exercise that interop surface end-to-end through the AOT
pipeline, so you can copy a file, change one line, and rebuild.
