Getting Started
===============


## Installation

Gloat has **zero dependencies** - everything installs automatically on first
use.


### Clone and Setup

For Bash, Fish or Zsh:

```bash
git clone https://github.com/gloathub/gloat
source gloat/.rc
gloat --help
```

The `source gloat/.rc` command adds the `gloat` command to your PATH, enables
the `man gloat` help and sets up `gloat` tab completion.

On first run, Gloat will automatically install all required tools (Go, Glojure,
YAMLScript, Babashka, etc) to `.cache/.local/` within the project directory.
Just run `gloat --help` once to complete the setup.


### Permanent Installation

To make Gloat available in all terminal sessions, add this to your shell's rc
file (`~/.bashrc`, `~/.fishrc` or `~/.zshrc`):

```bash
source /path/to/gloat/.rc
```


## Basic Usage


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
| `js`   | `-t js` with `.wasm` | WebAssembly (JavaScript) |


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

Anyone can build it with just `make` - Go is automatically installed.


## Cross-Compilation

Compile for different platforms using `-p OS/ARCH`:

```bash
# Linux targets
gloat app.ys -o app-linux -p linux/amd64
gloat app.ys -o app-arm -p linux/arm64

# macOS targets
gloat app.ys -o app-mac -p darwin/amd64
gloat app.ys -o app-mac-m1 -p darwin/arm64

# Windows targets
gloat app.ys -o app.exe -p windows/amd64

# WebAssembly
gloat app.ys -o app.wasm -p wasip1/wasm    # WASI
gloat app.ys -o app.wasm -p js/wasm        # JavaScript
```


### Supported Platforms

| OS | Architectures |
|----|---------------|
| `linux` | `amd64`, `arm64`, `386`, `arm` |
| `darwin` | `amd64`, `arm64` |
| `windows` | `amd64`, `arm64`, `386` |
| `freebsd` | `amd64`, `arm64`, `386` |
| `wasip1` | `wasm` |
| `js` | `wasm` |

Run `go tool dist list` to see all supported targets.


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
```


## Shared Libraries

Compile to shared libraries for FFI integration:

```bash
# Linux
gloat lib.clj -o libmylib.so

# macOS
gloat lib.clj -o libmylib.dylib

# Windows
gloat lib.clj -o mylib.dll -p windows/amd64
```

This generates both the library file and a `.h` header file for C bindings.


## The Make Shell

All Gloat dependencies are only accessible to Makefile rules, not your normal
shell.
To run commands like `go`, `ys`, or `bb` directly:

```bash
make shell
```

This starts a subshell with all tools in your PATH.
Your prompt will change to indicate you're in the Make shell.
Press Ctrl-D or type `exit` to return to your normal shell.


## Command-Line Options

```
-t, --to=FORMAT        Output format (inferred from -o)
-o, --out=FILE         Output file or directory
-p, --platform=OS/ARCH Cross-compile target
--ns=NAMESPACE         Override namespace
-r, --run              Compile to temp binary and run it

-f, --force            Overwrite existing output files
-v, --verbose          Print timing for each compilation step
-q, --quiet            Suppress progress messages

-h, --help             Show help
--version              Show version
```


## Next Steps

- [Try the Demo](demo.md) - Interactive browser-based demo
- [Browse Examples](examples.md) - See what you can build
- [GitHub Repository](https://github.com/gloathub/gloat) - Source and
  documentation
