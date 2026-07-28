Getting Started
===============


## Installation

There are two minimal ways to install Gloat:

### Source Installer

For Bash or Zsh:

```bash
source <(curl -sL gloathub.org/install)
```

For Fish:

```fish
curl -sL gloathub.org/install | source
```

### Makefile Installer

```bash
make -f <(curl -sL gloathub.org/make) install
```

### Clone and Setup

```bash
git clone https://github.com/gloathub/gloat
source gloat/.rc
gloat --help
```

For full installation instructions, options, and uninstall steps, see the
[Install](gloat-install.md) page.


## Basic Usage


### Read from Standard Input

Source-consuming commands read standard input when the source path is omitted.
An explicit `-` has the same meaning:

```bash
# These commands are equivalent
cat app.clj | gloat -t clj
cat app.clj | gloat -t clj -

# Save compiled stdin to a file
cat app.clj | gloat -o app.go
```

Running `gloat` without a command still displays help.


### Compile to Binary

By default, Gloat compiles to a native executable:

```bash
# Create ./app binary
gloat app.clj

# Specify output name
gloat app.clj -o myapp

# From YAMLScript source
gloat program.ys -o program
```


### View Intermediate Formats

Output any stage of the compilation pipeline:

```bash
# View generated Clojure
gloat code.ys -t clj

# View generated Glojure
gloat code.ys -t glj

# View generated Go
gloat code.ys -t go
```


### Format and Syntax Highlight Clojure

Use `-F` / `--fmt` to format Clojure with zprint. Formatting writes plain text
to standard output:

```bash
gloat -F code.clj
cat code.clj | gloat --fmt
```

Use `-w` / `--width` to set zprint's formatting width. It requires `--fmt`:

```bash
gloat -F -w 40 code.clj
gloat -FCw40 code.clj | less -R
```

Use `-C` / `--color` for the Glojure REPL's ANSI syntax colors and rainbow
brackets. Coloring is always enabled, including when output is piped or
redirected:

```bash
gloat -C code.clj
gloat --color code.clj | less -R
```

Combine the options in either order to format first and then color:

```bash
gloat -FC code.clj | less -R
cat code.clj | gloat --color --fmt | less -R
```


### Save to Files

Use `-o` to save output to a file:

```bash
# File format inferred from extension
gloat app.ys -o app.clj    # Save as Clojure
gloat app.ys -o app.glj    # Save as Glojure
gloat app.ys -o app.go     # Save as Go
```

Or use the shorthand `-t .ext` syntax:

```bash
gloat app.ys -t .clj       # Creates app.clj
gloat app.ys -t .glj       # Creates app.glj
gloat app.ys -t .go        # Creates app.go
```


## Output Formats

Gloat supports multiple output formats:

| Format | Flag | Description |
|--------|------|-------------|
| `bin`  | `-t bin` or no extension | Native executable (default) |
| `clj`  | `-t clj` or `.clj` | Clojure source |
| `bb`   | `-t bb` or `.bb` | Babashka self-contained script |
| `glj`  | `-t glj` or `.glj` | Glojure source |
| `go`   | `-t go` or `.go` | Go source |
| `dir`  | `-o path/` | Portable Go project directory |
| `lib`  | `.so` or `.dylib` | Shared library |
| `wasm` | `.wasm` | WebAssembly (WASI) |
| `js`   | `-t js` with `.wasm` | WebAssembly (JavaScript, browser-ready with `-Xhtml`) |


## Compilation Engines

Use `-E` / `--engine` to select a compilation engine. Run `gloat --engines`
to list the available engines and implementation status.

The `graalvm` engine compiles self-contained, namespaced Clojure programs to
host-native executables with GraalVM Native Image:

```bash
gloat --engine=graalvm app.clj
gloat -Egraalvm app.clj -o app
gloat -Egraalvm --run app.clj -- arg1 arg2
```

GraalVM and Leiningen are installed in Gloat's local cache the first time this
engine is used. This initial implementation supports Clojure binary output
only. It does not support YAMLScript, shared libraries, cross-compilation,
`gljdeps.edn`, Go modules, or `-X` processing extensions.


## Directory Output

Create a self-contained Go project directory:

```bash
gloat app.ys -o build/
```

This generates:

```
build/
├── Makefile           # Makes-based build (auto-installs Go)
├── go.mod             # Go module definition
├── main.go            # Entry point
└── pkg/app/core/      # Glojure runtime code
```

Anyone can build it with just `make` - Go is automatically installed (within
the build directory).


## Cross-Compilation

Compile for different platforms using `-p OS/ARCH`:

```bash
# Linux targets
gloat app.ys -o app-linux --platform=linux/amd64
gloat app.ys -o app-arm --platform=linux/arm64

# macOS targets
gloat app.ys -o app-mac --platform=darwin/amd64
gloat app.ys -o app-mac-m1 --platform=darwin/arm64

# Windows targets
gloat app.ys -o app.exe --platform=windows/amd64

# WebAssembly
gloat app.ys -o app.wasm --platform=wasip1/wasm    # WASI
gloat app.ys -o app.wasm --platform=js/wasm        # JavaScript
```


### Supported Platforms

| OS | Architectures |
|----|---------------|
| `linux` | `amd64`, `arm64`, `386`, `arm` |
| `darwin` | `amd64`, `arm64` |
| `windows` | `amd64`, `arm64`, `386` |
| `freebsd` | `amd64`, `arm64`, `386` |
| `openbsd` | `amd64`, `arm64` |
| `netbsd` | `amd64`, `arm64` |
| `wasip1` | `wasm` |
| `js` | `wasm` |

Run `gloat --platforms` to see all supported targets.


## Compile and Run

Use `--run` to compile to a temporary binary and execute it:

```bash
# Compile and run (no binary kept)
gloat --run app.ys

# Pass arguments to the program
gloat --run app.ys -- arg1 arg2
```


## WebAssembly Output

Create Wasm modules for browser or WASI environments:

```bash
# WASI target (default for .wasm)
gloat app.ys -o app.wasm

# JavaScript target (for browsers)
gloat app.ys -o app.wasm -t js

# JavaScript target with HTML page for browser
gloat app.ys -o app.js -Xhtml
gloat app.ys -o app.js -Xhtml='arg1 arg2'
```

Adding `-Xhtml` generates `app.html` alongside the WASM file.
The HTML page has the Go WASM runtime inlined and is ready to serve.
Use `python3 -m http.server` (gloat will print the command) since
`fetch()` requires HTTP, not `file://`.


## Shared Libraries

Compile to shared libraries for FFI integration:

```bash
# Linux
gloat lib.clj -o libmylib.so

# macOS
gloat lib.clj -o libmylib.dylib

# Windows
gloat lib.clj -o mylib.dll

# Use the let-go bytecode VM
gloat -Elgvm lib.clj -o libmylib.so

# Use native let-go lowering with VM fallback
gloat -Elglvm lib.clj -o libmylib.so
```

This generates both the library file and a `.h` header file for C bindings.
The default `glj` engine and both VM-backed let-go engines support shared
libraries.

Check out the [FFI bindings examples](
https://github.com/gloathub/gloat/tree/main/demo/so-bindings) for over 20
languages.

These are all working code and bind to an example shared library written in
YAMLScript and compiled with Gloat.
Every programming language that is needed is auto-installed by the
[Makes](https://github.com/makeplus/makes) system.

```bash
# Run the standard language bindings
make test-so-bindings

# Include the slow Haskell and Lua bindings
make -C demo/so-bindings test slow=1

# Test the bindings with either let-go engine
GLOAT_ENGINE=lgvm make -C demo/so-bindings test
GLOAT_ENGINE=lglvm make -C demo/so-bindings test
```


## The Gloat Shell

All Gloat dependencies are only accessible to Makefile rules, not your normal
shell.
To run commands like `go`, `glj`, `ys`, or `bb` directly:

```bash
gloat --shell
```

This starts a subshell with all tools in your PATH.
Your prompt will change to indicate you're in the Gloat subshell.
Press Ctrl-D or type `exit` to return to your normal shell.


## Command-Line Options

```
-o, --out ...    Output file or directory
-f, --force      Overwrite existing output files

-t, --to ...     Output format (inferred from -o; see --formats)
--formats        List available output formats

--ns ...         Override namespace
--module ...     Go module name (e.g., github.com/user/project)

-E, --engine ... Compilation engine: glj, graalvm, lgvm, lglvm, or lgl (default: glj)
--engines        List available compilation engines

--platform ...   Cross-compile (e.g., linux/amd64; see --platforms)
--platforms      List available cross-compilation platforms

-X, --ext ...    Enable a processing extension (see --extensions)
--extensions     List available processing extensions

-r, --run        Compile and run (pass program args after --)
-T, --time       With --run: print the run time (not compile) to stderr

--repl           Start REPL client; see 'man gloat-repl'
--nrepl          Start nREPL server; see 'man gloat-repl'
--srepl          Start socket REPL server; see 'man gloat-repl'
--deps ...       Path to gljdeps.edn (Go module deps; AOT or REPL)
--classpath ...  Classpath for REPL load paths (e.g. . or src:test)

-C, --color      Syntax highlight Clojure code
-F, --fmt        Format Clojure code w/ zprint
-w, --width ...  Width for --fmt formatting

--shell          Start a sub-shell or run a command (-- cmd...)
--shell-all      Like --shell but install all dev tools

--complete ...   Generate shell completion script (bash, fish, zsh)
--which ...      Print path to command that gloat uses: go, glj, etc

-v, --verbose    Print timing for each compilation step
-q, --quiet      Suppress progress messages

--upgrade        Upgrade gloat (use --upgrade=v1.2.3 to pin a version)
--reset          Remove all cached dependencies and reinstall

-h, --help       Show this help
--version        Show version
```


## Next Steps

- [Try the Demo](../demo.md) - Interactive browser-based demo
- [Browse Examples](../examples.md) - See what you can build
- [GitHub Repository](https://github.com/gloathub/gloat) - Source and
  documentation
