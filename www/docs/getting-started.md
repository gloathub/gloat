Getting Started
===============


## Installation

Gloat has **zero dependencies** - everything installs automatically on first
use.

There are two main ways to get started: the one-line installer or cloning the
repository and sourcing the `.rc` file.


### One-Line Installer

Install `gloat` (or `glj`) to `~/.local/bin/` with a single command:

```bash
make -f <(curl -sL gloathub.org/make) install
```

Installing `gloat` clones the gloat repository to `~/.local/share/gloat`,
creates a symlink at `~/.local/bin/gloat`, and installs all the required
dependencies automatically.

Make sure `~/.local/bin` is in your `PATH`, then run `gloat --help`.

**Options:**

```bash
# Install to a custom prefix
make -f <(curl -sL gloathub.org/make) install PREFIX=~/.gloat

# Install a specific version
make -f <(curl -sL gloathub.org/make) install VERSION=v0.1.8

# Also install the Glojure glj (prebuilt binary) command
make -f <(curl -sL gloathub.org/make) install-glj

# Uninstall
make -f <(curl -sL gloathub.org/make) uninstall
make -f <(curl -sL gloathub.org/make) uninstall-glj

# Show the installer Makefile rules:
make -f <(curl -sL gloathub.org/make) help
```

> **Note**: This installation method does not enable gloat shell completion.
> For that, run `source <(gloat --complete bash)` (or `fish`/`zsh`) in your
> shell's rc file after installation.


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

To make Gloat available permanently, simply add this to your shell's rc file
(`~/.bashrc`, `~/.fishrc` or `~/.zshrc`):

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
```


## Shared Libraries

Compile to shared libraries for FFI integration:

```bash
# Linux
gloat lib.clj -o libmylib.so

# macOS
gloat lib.clj -o libmylib.dylib

# Windows
gloat lib.clj -o mylib.dll
```

This generates both the library file and a `.h` header file for C bindings.

Check out the [FFI bindings examples](
https://github.com/gloathub/gloat/tree/main/demo/so-bindings) for over 20
languages.

These are all working code and bind to an example shared library written in
YAMLScript and compiled with Gloat.
You can run them all with `make test-so-bindings`.
Every programming language that is needed is auto-installed by the
[Makes](https://github.com/makeplus/makes) system.


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
-t, --to ...     Output format (inferred from -o; see --formats)
-o, --out ...    Output file or directory

--platform ...   Cross-compile (e.g., linux/amd64; see --platforms)
-X, --ext ...    Enable a processing extension (see --extensions)

--ns ...         Override namespace
--module ...     Go module name (e.g., github.com/user/project)

--formats        List available output formats
--extensions     List available processing extensions
--platforms      List available cross-compilation platforms
--complete ...   Generate shell completion script (bash, zsh, fish)
--shell          Start a sub-shell with gloat tools on PATH

-r, --run        Compile and run (pass program args after --)
-f, --force      Overwrite existing output files
-v, --verbose    Print timing for each compilation step
-q, --quiet      Suppress progress messages

-h, --help       Show this help
--version        Show version
```


## Next Steps

- [Try the Demo](demo.md) - Interactive browser-based demo
- [Browse Examples](examples.md) - See what you can build
- [GitHub Repository](https://github.com/gloathub/gloat) - Source and
  documentation
