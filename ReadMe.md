gloat - Glojure AOT Tool
========================

[![Try Gloat Live Demo](
https://img.shields.io/badge/Try_Gloat-Live_Demo-blue?logo=github)](
https://codespaces.new/gloathub/gloat?quickstart=1)

Compile Clojure or YS to Go code or native binaries


## Synopsis

Before using the **Try Gloat - Live Demo** badge above, read the
[Gloat Live Demo](#gloat-live-demo) section below.

```bash
# Compile to native binary (default)
gloat app.clj                   # Creates ./app binary
gloat app.clj -o myapp          # Creates ./myapp binary

# Output intermediate formats
gloat app.ys -t clj             # Clojure to stdout
gloat app.ys -t glj             # Glojure to stdout
gloat app.ys -t go              # Go to stdout

# Create files with -t .ext shorthand
gloat app.ys -t .clj            # Creates app.clj
gloat app.ys -t .glj            # Creates app.glj
gloat app.ys -t .go             # Creates app.go

# Create a portable Go project directory
gloat app.ys -o build/          # Creates build/ with Makefile

# Cross-compile
gloat app.ys -o app-linux -p linux/amd64
gloat app.ys -o app.exe -p windows/amd64

# Compile and run
gloat --run app.ys              # Compile and run (no binary kept)
gloat --run app.ys -- arg1 arg2 # Pass arguments to program

# WebAssembly targets
gloat app.ys -o app.wasm        # WASI target
gloat app.ys -o app.wasm -t js  # JavaScript target
```


## Description

Gloat compiles Clojure or YAMLScript source files to any of these forms:

* Native binaries
  * Cross compile to many OS/Arch environments
* Web Assembly
  * Wasi P1
  * JS Wasm
* Shared libraries (`.so`, `.dylib`, `.dll`)
  * With `.h` header files for FFI binding
* Go source files
* Standalone Go build directories

```
.ys  →  ys -c   →  .clj
                    ↓
.clj  ────────→  rewrite  →  .glj
                              ↓
.glj  ──────────────────→  glj compile  →  .go
                                            ↓
                                       go build  →  binary/wasm
```

The tool has **zero external dependencies**.
All required tools (go, glj, ys, bb) are automatically installed on first use
via the [Makes](https://github.com/makeplus/makes) build system.


## Gloat Live Demo

The gloat repository comes with a lot of example programs.
You can find them in the `example/clojure`, and `example/yamlscript`
directories.

A great way to view the demos is to start the Gloat Demo Webpage Server with:
```
$ make demo-server
...some output...
Starting server on http://localhost:8080
Press Ctrl+C to stop
```

That serves a webpage that will let you try all of the examples interactively.

In the webpage you can choose an example and then click **Compile**, which
sends it back to the local server, turns it into a Wasm file that can run in
the browser.
It will also show you the Glojure and the Go code that was generated to create
the Wasm.
After a program compiles, you can run it with the **Run** button.


### Run the Demo Now!

At the top of this file, you may have noticed this badge:
[![Gloat Live Demo](
https://img.shields.io/badge/Try_Gloat-Live_Demo-blue?logo=github)](
https://codespaces.new/gloathub/gloat?quickstart=1)

GitHub users can click that and see the demo without cloning this repository.
It opens in a GitHub Codespaces session, which starts as an empty VScode editor
session.
If you wait a couple minutes (literally) the demo will start in an editor pane.

> **Notes:**
> The demo page **literally takes 1-2 minutes to start**.
> Use Cmd/Ctrl + Shift + P -> View Creation Log to see full logs.
> After the demo starts in the editor, you can pop it out to a separate browser
> pane.


### Using `make` to run examples

Try these:
```
make run FILE=example/yamlscript/dragon-curve.ys
make run FILE=example/clojure/even-or-odd.clj a='7 42 31337'
```


## Installation

Clone this repository and source the `.rc` file:

```bash
git clone https://github.com/gloathub/gloat
source gloat/.rc
gloat --help
```

The first time you run the `gloat` command, all its dependencies will be
installed under the `gloat/.cache/.local/` directory.

To make `gloat` a permanent install, add this to your shell's rc file:

```
source /absolute/path/to/gloat/.rc
```


## Output Formats

| Format | Flag | Description |
|--------|------|-------------|
| `bin`  | `-t bin` or no extension | Native executable (default) |
| `clj`  | `-t clj` | Clojure source |
| `bb`   | `-t bb` | Babashka self-contained script |
| `glj`  | `-t glj` | Glojure source |
| `go`   | `-t go` | Go source |
| `dir`  | `-o path/` | Portable Go project directory |
| `lib`  | `.so` or `.dylib` extension | Shared library |
| `wasm` | `.wasm` extension | WebAssembly (WASI) |
| `js`   | `-t js` with `.wasm` | WebAssembly (JavaScript) |

The output format is inferred from the `-o` extension, or can be explicitly
set with `-t`.


## Directory Output

When outputting to a directory (`-o build/`), gloat generates a self-contained
Go project that builds with zero pre-installed dependencies:

```
build/
├── Makefile
├── go.mod
├── main.go
└── pkg/
    ├── app/core/loader.go
    ├── yamlscript/util/loader.go
    ├── ys/dwim/loader.go
    ├── ys/std/loader.go
    └── ys/v0/loader.go
```

Anyone can build it with just `make` - Go is automatically installed.


## Cross-Compilation

Use `-p OS/ARCH` to cross-compile for different platforms:

```bash
# Linux targets
gloat app.ys -o app-linux-amd64 -p linux/amd64
gloat app.ys -o app-linux-arm64 -p linux/arm64

# macOS targets
gloat app.ys -o app-darwin-amd64 -p darwin/amd64
gloat app.ys -o app-darwin-arm64 -p darwin/arm64

# Windows targets
gloat app.ys -o app.exe -p windows/amd64
gloat app.ys -o app-win-arm64.exe -p windows/arm64

# WebAssembly targets
gloat app.ys -o app.wasm -p wasip1/wasm    # WASI
gloat app.ys -o app.wasm -p js/wasm        # JavaScript
```

Common platform targets:

| OS | Architectures |
|----|---------------|
| `linux` | `amd64`, `arm64`, `386`, `arm` |
| `darwin` | `amd64`, `arm64` |
| `windows` | `amd64`, `arm64`, `386` |
| `freebsd` | `amd64`, `arm64`, `386` |
| `wasip1` | `wasm` |
| `js` | `wasm` |

Run `go tool dist list` to see all supported targets.


## Options

```
-t, --to=FORMAT     Output format (inferred from -o)
-o, --out=FILE      Output file or directory
-p, --platform=OS/ARCH  Cross-compile target
--ns=NAMESPACE      Override namespace
-r, --run           Compile to temp binary and run it

-f, --force         Overwrite existing output files
-v, --verbose       Print timing for each compilation step
-q, --quiet         Suppress progress messages

-h, --help          Show help
--version           Show version
```


## The Make Shell

The Makefile is set up so that you don't need to install any dependencies.
They get installed the first time they are needed by a rule invoked by a `make`
command that you run.

However, these tools are only accessible to Makefile rules.
They are not availabe for you to run directly in your shell.

Sometimes you want to run these commands like `go`, `ys` and `bb` directly in
your shell.
In that case, just run: `make shell`.

This will put you in a subshell with all of those commands available.
Your `PS1` shell prompt will be changed so that it will be easy to know you are
in such a subshell.

To leave this environment and get back to the shell that you started in, just
press Ctrl-D or run the `exit` command.


## Copyright and License

Copyright 2026 - Ingy dot Net

MIT License - See [License](License) file.
