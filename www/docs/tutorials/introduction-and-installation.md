Introduction and Installation
=============================

This is the starting point for the tutorial series.
It covers what Gloat actually is, what it can produce, and how to get it
working on your machine.

By the end you'll have a working `gloat` command and know where it put
everything.
The next tutorial puts it to work.

**Time**: about 10 minutes

**You'll need**: a Unix-like shell (Linux, macOS, WSL)


## What Gloat Is

Gloat is a multi-purpose automation tool for Clojure.
Its main purpose is to compile Clojure code into native binary executables but
it can do many other things as well:

* Compile Clojure into Go source code
* Compile Clojure into WebAssembly (WASI or browser)
* Compile Clojure into a shared library with a C header
* Compile Clojure into a Go project directory you can build anywhere
* Cross-compile Clojure into binaries for other platforms
* Compile YAMLScript into any of the above

It also offers a [REPL client](/doc/gloat-repl.md) for both the terminal and
the browser.
This REPL goes far beyond the standard Clojure REPL, with features like:

* Syntax highlighting similar to Calva's
* Tab completion for namespaces, vars, and keywords
* Multi-line form editing with history and search
* REPL state sharing via base64 encoded URLs
* nREPL and socket REPL support

Gloat was originally an automation tool for the Glojure dialect, but it has
been extended to also support [let-go](https://github.com/nooga/let-go) and
[GraalVM](https://www.graalvm.org/), with many other Clojure dialect "engines".
planned for the future.
It is designed to be a single interface for quickly compiling Clojure to
binaries and the other formats listed above.

Gloat also has little handy extras like `--format` and `--color` for better
viewing of Clojure files.
See `gloat --help` and `man gloat` for the full list of options and commands.


## What You Can Produce

| Output                    | How you ask for it               |
|---------------------------|----------------------------------|
| Native executable         | `gloat app.clj`                  |
| Cross-compiled executable | `gloat app.clj -p windows/amd64` |
| WebAssembly (WASI)        | `gloat app.clj -o app.wasm`      |
| WebAssembly (browser)     | `gloat app.clj -o app.js -Xopen` |
| Shared library + C header | `gloat app.clj -o libapp.so`     |
| Go project directory      | `gloat app.clj -o build/`        |
| Go source code file       | `gloat app.clj -t go`            |

`-o` / `--out` says where the result goes; `-t` / `--to` says what to produce,
and is usually inferred from the `-o` extension.
See `gloat --formats`.

`-E` / `--engine` picks the Clojure implementation to compile through - Glojure
by default, plus let-go and GraalVM variants.
See `gloat --engines`.

Gloat cross-compiles to 25 platform combinations.
Shared libraries mean anything with an FFI can call your Clojure code - the
repository has
[working bindings for 23 languages](../examples.md#shared-library-bindings).


## Install

Gloat is really just a wrapper around several other tools.
The `gloat` command itself is a combination of Bash and Babashka.
It makes heavy use of a Makefile system that knows how to pull in every other
dependency it needs and when it needs it.

The nice thing about Gloat is that it is trivial to install and doesn't have any
preinstall requirements.
Not even Babashka or Go!

> Technically, you do need `git`, `curl`, `make` and `bash` to be installed,
> but that's already installed almost everywhere.

There are two supported routes.
Both end with a `gloat` command on your `PATH`, man pages, and tab completion.

The best way to install gloat is to clone the repository and then source the
`.rc` file from your shell's rc file.

If you just want to try it out, the quick install is a one-liner that does
everything for you.

For Bash or Zsh:

```bash
source <(curl -sL gloathub.org/install)
```

For Fish:

```fish
curl -sL gloathub.org/install | source
```

This will install `gloat` under `~/.local/share/gloat/` and add it to your
`PATH` for the current shell session.

You'll also have tab completion and man pages available immediately.

Run `gloat --help` to see the full list of commands and options.

See [the installation page](../doc/gloat-install.md) for full details.


## Next

You have a working toolchain.
Time to compile something.
