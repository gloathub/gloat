gloat - [Glojure](https://github.com/glojurelang/glojure) AOT Tool
==================================================================

[![Try Gloat Live Demo](
https://img.shields.io/badge/Try_Gloat-Live_Demo-blue?logo=github)](
https://codespaces.new/gloathub/gloat?quickstart=1)

<img src="www/docs/img/gloat.jpeg" alt="Gloat Mascot" width="400">

[Gloat](https://gloathub.org) compiles [Clojure](https://clojure.org) or
[YAMLScript](https://yamlscript.org) to [Go](https://go.dev) code or native
binaries

> Cross-compiles to 20+ platforms including [Wasm](https://webassembly.org/).


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
gloat app.ys -o build/          # Creates build/ directory

# Cross-compile
gloat app.ys -o app-linux --platform=linux/amd64
gloat app.ys -o app.exe --platform=windows/amd64

# Compile and run
gloat --run app.ys              # Compile and run (no binary kept)
gloat --run app.ys -- <args...> # Pass arguments to program

# WebAssembly targets
gloat app.ys -o app.wasm        # WASI target
gloat app.ys -o app.wasm -t js  # JavaScript target
```


## Description

Gloat compiles Clojure or YAMLScript source files to any of these forms:

* Native binaries
  * Cross compile to 20+ OS/Arch environments
* Web Assembly
  * Wasi P1 - Run on server
  * JS Wasm - Run in browser
* Shared libraries (`.so`, `.dylib`, `.dll`)
  * With `.h` header files for FFI binding
* Go source files
* Standalone Go build directories
* Standalone Babashka (Clojure) files

```txt
.ys  →  ys -c   →  .clj
                    ↓
.clj  ────────→  rewrite  →  .glj
                              ↓
.glj  ──────────────────→  glj compile  →  .go
                                            ↓
                                       go build  →  binary/wasm
```

The tool has **zero external dependencies**.
All required tools (bb, glj, go, ys) are installed the first time you run
`gloat` via the [Makes](https://github.com/makeplus/makes) build system.

All of these tools will be installed local to the gloat repository under
`/path/to/gloat/.cache/.local/` and you will be prompted about it first.


## Gloat Live Demo

The gloat repository comes with a lot of demo programs.
You can find them in the `demo/clojure/`, and `demo/yamlscript/`
directories.

A great way to view the demos is to start the Gloat Demo Webpage Server with:
```
$ make serve-demo
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
If you **wait a couple minutes (literally)** the demo will start in an editor
pane.

> **Notes:**
> The demo page **literally takes 1-2 minutes to start**.
> Use Cmd/Ctrl + Shift + P -> View Creation Log to see full logs.
> After the demo starts in the editor, you can pop it out to a separate browser
> pane.


### Using `gloat --run` to run examples

Try these:
```
gloat --run demo/yamlscript/dragon-curve.ys
gloat -r demo/clojure/even-or-odd.clj -- 7 42 31337
```

To pass options to a program run with `gloat --run`, put the after a `--` arg.


## Shared Library Bindings

Gloat can compile YAMLScript to shared libraries (`.so`/`.dylib`/`.dll`)
with auto-generated C header files, enabling FFI (Foreign Function Interface)
bindings from **23 programming languages**.

The [`demo/so-bindings/`](
https://github.com/gloathub/gloat/tree/main/demo/so-bindings) directory
contains working examples for every supported language, all calling the same
shared library compiled from a single YAMLScript source file
(`demo/so-bindings/example.ys`).

The library exports 6 functions: `factorial`, `greet`, `repeat_string`,
`shout_it`, `maybe`, and `sort_json_array`.

Supported languages:
Ada, C, C++, C#, Crystal, D, Dart, Delphi, Fortran, Go, Haskell,
Java, Julia, Lua, Nim, Node.js, Perl, Python, Raku, Ruby, Rust, V,
Zig

```bash
# Build the shared library and run all 23 language bindings
make test-so-bindings

# Run a single language binding
make -C demo/so-bindings/python run
```


## Installation


### One-Line Installer

Install gloat to `~/.local` with a single command:

```bash
make -f <(curl -sL gloathub.org/make) install
```

This clones the gloat repository to `~/.local/share/gloat`, creates a symlink
at `~/.local/bin/gloat`, and installs all dependencies automatically.

Make sure `~/.local/bin` is in your `PATH`, then run `gloat --help`.

Run `make -f <(curl -sL gloathub.org/make) help` to see all available targets.

```bash
# Install to a custom prefix
make -f <(curl -sL gloathub.org/make) install PREFIX=~/mytools

# Install a specific version
make -f <(curl -sL gloathub.org/make) install VERSION=v0.1.2

# Also install the glj compiler
make -f <(curl -sL gloathub.org/make) install-glj

# Uninstall
make -f <(curl -sL gloathub.org/make) uninstall
```


### Clone and Setup

Clone this repository and source the appropriate rc file for your shell:

```bash
$ git clone https://github.com/gloathub/gloat

# For Bash, Fish or Zsh
$ source gloat/.rc

$ gloat --help
==> Installing gloat dependencies (bb, glj, go, ys) locally into:

    /home/ingy/src/gloat/worktree/clojure-rewrite/.cache/.local/

Press Enter to continue (or Ctrl-C to cancel)...
...installs stuff within the gloat repo dir...

$ man gloat
```

The first time you run the `gloat` command, all its dependencies will be
installed under the `/path/to/gloat/.cache/.local/` directory.

Sourcing the rc file adds `gloat` to your PATH and automatically loads shell
completions for your shell.

To make `gloat` a permanent install, add this to your shell's rc file:

```bash
# For Bash: add to ~/.bashrc
# For Zsh: add to ~/.zshrc
source /absolute/path/to/gloat/.rc

# For Fish: add to ~/.config/fish/config.fish
source /absolute/path/to/gloat/.fishrc
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
├── Makefile           # Makes-based build (auto-installs Go)
├── go.mod             # Go module definition
├── main.go            # Entry point
└── pkg/app/core/      # Glojure runtime code
```

Anyone can build it with just `make` - Go is automatically installed.


## Cross-Compilation

Use `--platform=OS/ARCH` to cross-compile for different platforms:

```bash
# Linux targets
gloat app.ys -o app-linux-amd64 --platform=linux/amd64
gloat app.ys -o app-linux-arm64 --platform=linux/arm64

# macOS targets
gloat app.ys -o app-darwin-amd64 --platform=darwin/amd64
gloat app.ys -o app-darwin-arm64 --platform=darwin/arm64

# Windows targets
gloat app.ys -o app.exe --platform=windows/amd64
gloat app.ys -o app-win-arm64.exe --platform=windows/arm64

# WebAssembly targets
gloat app.ys -o app.wasm --platform=wasip1/wasm    # WASI
gloat app.ys -o app.wasm --platform=js/wasm        # JavaScript
```

Common platform targets:

| OS        | Architectures                  |
|-----------|--------------------------------|
| `linux`   | `amd64`, `arm64`, `386`, `arm` |
| `darwin`  | `amd64`, `arm64`               |
| `windows` | `amd64`, `arm64`, `arm`, `386` |
| `freebsd` | `amd64`, `arm64`, `386`        |
| `openbsd` | `amd64`, `arm64`               |
| `netbsd`  | `amd64`, `arm64`               |
| `wasip1`  | `wasm`                         |
| `js`      | `wasm`                         |

Less common platform architectures:

| OS          | Architectures                              |
|-------------|--------------------------------------------|
| `linux`     | `ppc64le`, `s390x`, `riscv64`, `mips64le`  |
| `dragonfly` | `amd64`                                    |


## Extensions

Extensions add post-compilation processing steps.
Enable them with `-X` (or `--ext`):

    gloat app.ys -o app.js -Xhtml
    gloat app.ys -o app.wasm -Xprune,gzip

Multiple extensions can be combined with commas, and some accept a
value with `=`:

    gloat app.ys -o app.js -Xserve,html=100

Run `gloat --extensions` to list all available extensions.


### brotli

Compress WASM output with [Brotli](https://github.com/google/brotli)
(auto-installed if needed).
Applies to `wasm` and `js` output formats.

    gloat app.ys -o app.wasm -Xbrotli


### deps

Print the dependency graph of the compiled program (implies `prune`).
With no value, prints a flat list; with `=tree`, prints a tree.

    gloat app.ys -o app -Xdeps          # Flat list
    gloat app.ys -o app -Xdeps=tree     # Tree view


### gzip

Compress WASM output with gzip (requires `gzip` on PATH).
Applies to `wasm` and `js` output formats.

    gloat app.ys -o app.wasm -Xgzip


### html

Generate a self-contained HTML page for running a JS/WASM module in
the browser.
Only valid with `js` format (`-o app.js`).

    gloat app.ys -o app.js -Xhtml
    gloat app.ys -o app.js -Xhtml='arg1 arg2'

This generates `app.html` alongside `app.js`, with the Go WASM
runtime (`wasm_exec.js`) inlined.
After generation, gloat prints the command to serve locally.

> **Note:** `fetch()` requires HTTP, not `file://`, so a local
> server is needed to run WASM in the browser.


### open

Open the page in your default browser after starting the server.
Implies `-Xserve` (which implies `-Xhtml`).
Only valid with `js` format.

    gloat app.ys -o app.js -Xopen
    gloat app.ys -o app.js -Xopen='arg1 arg2'

The implication chain: `-Xopen` → `-Xserve` → `-Xhtml`.


### prune

Prune unused `clojure.core` and YAMLScript runtime functions from
the compiled output, producing smaller binaries.
Applies to binary builds (`bin`, `lib`, `wasm`, `js`, `dir`).

    gloat app.ys -o app -Xprune
    gloat app.ys -o app.wasm -Xprune


### serve

Start a local HTTP server after building.
Implies `-Xhtml`.
Only valid with `js` format.

    gloat app.ys -o app.js -Xserve
    gloat app.ys -o app.js -Xserve='arg1 arg2'

Without explicit `-Xhtml`, the HTML page is served from a temporary
directory so the output directory is left clean.
With `-Xserve,html`, the HTML is generated alongside the output.


## Options

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

--shell          Start a sub-shell or run a command (-- cmd...)
--shell-all      Like --shell but install all dev tools
--complete ...   Generate shell completion script (bash, fish, zsh)

-r, --run        Compile and run (pass program args after --)
-f, --force      Overwrite existing output files
-v, --verbose    Print timing for each compilation step
-q, --quiet      Suppress progress messages

--upgrade        Upgrade gloat to the latest version
--reset          Remove all cached dependencies and reinstall

-h, --help       Show this help
--version        Show version
```


## Shell Completion

Gloat provides tab completion for all shells.
Completions are automatically loaded when you source `.rc` (bash/zsh) or
`.fishrc` (fish).

To manually generate or reload completions:

```bash
# Bash
eval "$(gloat --complete=bash)"

# Zsh
eval "$(gloat --complete=zsh)"

# Fish
gloat --complete=fish | source
```

Completions provide:
- All command-line options (short and long forms)
- Available values for `--to` (output formats)
- Available values for `--platform` (cross-compilation targets)
- Available values for `--ext` (processing extensions)
- File completion for source files (`.ys`, `.clj`, `.glj`)
- File completion for `--out` output paths


## The Gloat Shell

The Makefile is set up so that you don't need to install any dependencies.
They get installed the first time they are needed by a rule invoked by a `make`
command that you run.

However, these tools are only accessible inside the Makefile; to the rules that
get run when you use `make` commands.
In other words, they are NOT available for you to run directly in your shell.

Sometimes you want to run these commands like `go`, `glj`, `ys` and `bb`
directly in your shell.
Gloat provides two shell variants:

* `gloat --shell` — Starts a subshell with the core tools (bb, glj, go, ys)
  plus `wasmtime` on PATH.
  This is the everyday variant for compiling and running programs.

* `gloat --shell-all` — Like `--shell` but also installs all developer tools
  (gh, go-md2man, shellcheck, brotli, etc.).
  Use this when you need the full development environment.

Your shell prompt will be prefixed with `(gloat) ` so that you know you are in
the subshell.

To leave this environment and get back to the shell that you started in, just
press Ctrl-D or run the `exit` command.

Both variants also support running a single command without starting an
interactive shell, using `--` followed by the command:

```bash
# Run a command string via bash -c (single argument)
gloat --shell -- 'glj --version'

# Run a command with separate arguments (direct exec)
gloat --shell -- glj --version

# Use --shell-all for commands that need dev tools
gloat --shell-all -- go-md2man --help
```


## Advanced Configuration

### Resetting Dependencies

If your cached dependencies become corrupted or you want a clean reinstall,
run:

```bash
gloat --reset
```

This runs `make distclean`, removing the `.cache/` directory.
The next invocation of gloat will reinstall all dependencies from scratch.

### Building Glojure from Source

By default, gloat downloads a pre-built `glj` binary from GitHub releases.
If a pre-built binary is not available for your platform, or you need to test
unreleased Glojure changes, you can build `glj` from source.

Set `GLOJURE_FROM_SOURCE` as an environment variable:

```bash
export GLOJURE_FROM_SOURCE=true
```


## Copyright and License

Copyright 2026 - Ingy dot Net

MIT License - See [License](License) file.
