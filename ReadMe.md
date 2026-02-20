gloat - Glojure AOT Tool
========================

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


## Installation

Clone this repository and source the appropriate rc file for your shell:

```bash
$ git clone https://github.com/gloathub/gloat

# For Bash or Zsh
$ source gloat/.rc

# For Fish
$ source gloat/.fishrc

$ gloat --help
==> Installing gloat dependencies (bb, glj, go, ys) locally into:

    /home/ingy/src/gloat/worktree/clojure-rewrite/.cache/.local/

Press Enter to continue (or Ctrl-C to cancel)...
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



## Options

```
-t, --to=FORMAT         Output format (inferred from -o; see --formats)
-o, --out=FILE          Output file or directory

    --platform=OS/ARCH  Cross-compile (e.g., linux/amd64; see --platforms)
-X, --ext=EXT           Enable a processing extension (see --extensions)

    --ns=NAMESPACE      Override namespace
    --module=NAME       Go module name (e.g., github.com/user/project)

    --formats           List available output formats
    --extensions        List available processing extensions
    --platforms         List available cross-compilation platforms

-r, --run               Compile and run (pass program args after --)
-f, --force             Overwrite existing output files
-v, --verbose           Print timing for each compilation step
-q, --quiet             Suppress progress messages

-h, --help              Show help
    --version           Show version
    --complete=SHELL    Generate shell completion script (bash, zsh, fish)
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


## The Make Shell

The Makefile is set up so that you don't need to install any dependencies.
They get installed the first time they are needed by a rule invoked by a `make`
command that you run.

However, these tools are only accessible inside the Makefile; to the rules that
get run when you use `make` commands.
In other words, they are NOT available for you to run directly in your shell.

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
