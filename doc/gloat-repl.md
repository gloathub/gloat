# gloat-repl -- Gloat/Glojure REPL Reference

## Synopsis

```
gloat --repl[=VALUE]
gloat --nrepl[=VALUE]
gloat --srepl[=VALUE]
gloat (--repl|--nrepl|--srepl) --deps=gljdeps.edn
glj
```

## Description

The Gloat/Glojure REPL is a rich interactive environment for evaluating
Clojure expressions with full access to Go packages.
It is built on the `gloathub/go-readline` library (a fork of
`reeflective/readline`) and supports vi and emacs editing modes,
multiline editing with auto-indent, tab completion with namespace-aware
descriptions, ghost text autosuggestions, persistent history, job
control, and bracketed paste.

## Startup

On launch the REPL prints a banner with version and platform
information and quick-reference hints:

```
🐐 Gloat: 0.1.40 🐐
 Glojure: v0.6.4
      Go: 1.24.0 linux/amd64
   nREPL: nrepl://localhost:38291
   sREPL: localhost:35149
    Help: C-h or :repl/help
    Exit: C-d or :repl/exit

user=>
```

The `nREPL` and `sREPL` URLs are for the embedded servers automatically
started for every `gloat --repl` session (see **Embedded Servers**).

The prompt shows the current namespace (`user` by default).
Switch namespaces with `(ns my-ns)` and the prompt updates
accordingly.

## Editing Modes

The REPL defaults to **vi** editing mode.
All standard vi-insert and vi-command keybindings are available (h/j/k/l
navigation, word motions, text objects, visual mode, yank/put, etc.).

To switch to **emacs** mode, add the following to your `~/.inputrc`:

```
set editing-mode emacs
```

The REPL respects all standard inputrc settings.

You can also switch modes at runtime with `:repl/vi` and
`:repl/emacs` (see **REPL Commands** below).

## Multiline Editing

The REPL automatically detects incomplete expressions.
If you press Enter on an unfinished form (unclosed parentheses, missing
string delimiter, etc.), a continuation prompt appears:

```
user=> (defn greet [name]
...   (str "Hello, " name "!"))
#'user/greet
```

When the cursor is not at the end of the buffer, Enter inserts a
newline regardless of whether the expression is complete.
This lets you go back and add lines in the middle of a multiline form.
In vi-command mode, Enter always submits the expression.

New lines automatically inherit the leading whitespace of the current
line, keeping your code aligned.

## Indentation

**Tab** inserts two spaces when the cursor is at the beginning of a
line or follows whitespace.
This makes it easy to indent code inside multiline forms.

**Backspace** is indent-aware: when two consecutive spaces precede the
cursor, both are deleted at once to undo a single indent level.

When the cursor follows a symbol character, Tab triggers completion
instead (see below).

## Tab Completion

Pressing Tab after one or more symbol characters opens a completion
menu.
The common prefix of all matches is inserted first, then subsequent Tab
presses cycle through the remaining candidates.

Completions include:

- **Symbols** from the current namespace (includes referred vars).
  Each candidate shows the namespace it was defined in.
- **Namespace aliases** (e.g. `str/`) with an "alias" label.
- **Full namespace names** (e.g. `clojure.string/`) with a "namespace"
  label.
- **Qualified symbols** -- typing `str/` then Tab completes symbols
  within that namespace or alias.

### Examples

```
user=> doc<TAB>          ; completes to (doc ...) candidates
user=> ns-<TAB>          ; shows ns-name, ns-aliases, ns-map, ...
user=> str/<TAB>         ; shows clojure.string functions
user=> clojure.set/<TAB> ; shows clojure.set functions
```

## Key Bindings

The following keys have special behavior in the REPL.
Vi-command-mode bindings (h/j/k/l, w/b/e, etc.) work as expected and
are not listed here.

| Key | Action |
|-----|--------|
| **Tab** | Insert 2-space indent (after whitespace) or open completion menu (after symbol chars) |
| **Backspace** | Delete 2 spaces if both precede cursor, otherwise delete 1 character |
| **Enter** | Submit expression (cursor at end and expression complete) or insert newline; in vi-command mode, always submits |
| **Ctrl+C** | Cancel current input; on empty prompt, shows exit hint; press twice to exit |
| **Ctrl+D** | Show inline documentation for symbol under cursor; on empty prompt, exit the REPL. In emacs mode use **Ctrl+X Ctrl+D**. |
| **Ctrl+P** | Format, print and copy current form to clipboard. In emacs mode use **Ctrl+X Ctrl+P**. |
| **Ctrl+S** | Share by URL; also copy selected forms |
| **Ctrl+E** | Move cursor to end of line (vi-insert and emacs modes) |
| **Ctrl+R** | Reverse incremental history search |
| **Ctrl+Z** | Suspend the REPL process; resume with `fg` |
| **Ctrl+H** | Show inline help with key bindings and commands. In emacs mode use **Ctrl+X Ctrl+H**. |
| **Up** | Move up a line in multiline input; on first line, move to beginning of line; at beginning or end of buffer, navigate history |
| **Down** | Move down a line in multiline input; on last line, move to end of line; at end of buffer, navigate history |
| **Escape** | Switch to vi-command mode; if inline doc is showing, dismiss it instead |

## History

Command history is saved to `~/.glj_history` and persists across
sessions.
Use the Up and Down arrow keys (or `k` and `j` in vi-command mode) to
navigate previous entries.
Press **Ctrl+R** to start a reverse incremental search -- type a
substring and the most recent matching entry is recalled.

History entries can span multiple lines.

### Smart Arrow Navigation

The Up and Down arrows have context-sensitive behavior in multiline
forms:

- **Down** on the last line moves to end-of-line first, then to
  history on the next press.
- **Up** on the first line moves to beginning-of-line first, then to
  history on the next press.
- **Up** at the end of the buffer jumps straight to history, letting
  you scan through previous forms (including multiline ones) without
  scrolling line by line.

### Ghost Text (Autosuggestions)

As you type, the REPL shows faded "ghost text" after the cursor when
there is exactly one matching completion.
For example, typing `(ze` shows `ro?` ghosted, completing `zero?`.
Press **Right arrow** to accept the full suggestion, or **Ctrl+Right**
to accept just the next word.

This feature can be toggled off by adding `set history-autosuggest off`
to your `~/.inputrc`.

## Suspend and Resume

Press **Ctrl+Z** to suspend the REPL and return to your shell.
The REPL saves and restores terminal state automatically, so resuming
with `fg` returns you to a working prompt with no garbled output.

## Bracketed Paste

Bracketed paste is enabled by default.
When you paste a block of code from your clipboard, it is inserted
instantly as a single unit rather than being interpreted character by
character.
Carriage returns in pasted text are normalized to newlines.

## Exiting

There are three ways to exit the REPL:

- **Ctrl+D** -- on an empty prompt, sends EOF and exits immediately.
  On a non-empty prompt, shows inline documentation (see below).
- **:repl/exit** -- type this command and press Enter.
- **Ctrl+C twice** -- press Ctrl+C on an empty prompt to see a hint,
  then press it again to exit.

If you press Ctrl+C while editing an expression, it cancels the current
input and returns to a fresh prompt.

## Interrupting Evaluation

Press **Ctrl+C** during evaluation to interrupt a long-running
expression.
The REPL prints "Interrupted" and returns to the prompt.

## Syntax Highlighting

The REPL highlights Clojure syntax as you type.
Token types and their colors:

| Token | Examples | Color |
|-------|----------|-------|
| Strings | `"hello"` | Green |
| Keywords | `:foo`, `::bar`, `:ns/key` | Cyan |
| Comments | `; note` | Gray |
| Numbers | `42`, `3.14`, `1/3`, `0xFF` | Magenta |
| Booleans/nil | `true`, `false`, `nil` | Magenta |
| Special forms | `def`, `fn`, `let`, `if`, `when`, `cond` | Bold yellow |
| Core functions | `map`, `filter`, `reduce`, `str` | Blue |
| Symbols | everything else | Default |

Special forms include `def`, `defn`, `defmacro`, `fn`, `let`, `loop`,
`recur`, `if`, `when`, `cond`, `do`, `ns`, `require`, `import`,
`try`, `catch`, `throw`, and others.

Highlighting works correctly with multiline editing, ghost text, and
tab completion.

### Rainbow Brackets

Brackets (`()`, `[]`, `{}`) are colored based on nesting depth using
the same color scheme as Calva's rainbow brackets.
The colors cycle through seven levels:

| Depth | Color |
|-------|-------|
| 0 | Light gray |
| 1 | Blue |
| 2 | Salmon |
| 3 | Green |
| 4 | Purple |
| 5 | Gray |
| 6 | Orange |

At depth 7 the cycle repeats from light gray.
Matching open and close brackets always share the same color.

Mismatched brackets (e.g. `(` closed by `]`) and unmatched closing
brackets are shown with white text on a red background, making
errors easy to spot as you type.

## nREPL Server

Start an nREPL server with `gloat --nrepl`.
Editors like Calva and CIDER connect to the server for interactive
development.

### `--nrepl` Forms

| Form | Meaning |
|------|---------|
| `--nrepl` | Start server on localhost, random port |
| `--nrepl=7888` | Start server on localhost:7888 |
| `--nrepl=0.0.0.0:7888` | Bind to all network interfaces, port 7888 |
| `--nrepl=0.0.0.0` | Bind to all interfaces, random port |
| `--nrepl=.nrepl-port` | Random port, write port number to file |

The value heuristic:
all digits = port number,
contains `:` with trailing digits = host:port,
valid IP address = host with random port,
anything else = port file path.

The server prints its address on startup in the format that Calva
expects for jack-in:

```
nREPL server started on port 7888 on host localhost - nrepl://localhost:7888
```

Press Ctrl+C to shut down the server.
If a port file was specified, it is removed on shutdown.

### Editor Integration

**Calva (VS Code):**
Run `gloat --nrepl` in a terminal.
Use "Calva: Connect to a Running REPL Server" and enter the port.
Alternatively, configure Calva's jack-in to run `gloat --nrepl`.

**CIDER (Emacs):**
Run `gloat --nrepl` in a terminal, then `M-x cider-connect` and
enter the host and port.

### Supported nREPL Operations

The server implements the following nREPL ops:
clone, close, describe, eval, completions, interrupt, load-file,
ls-sessions.

## Socket REPL

Start a plain-text socket REPL with `gloat --srepl`.
Clients connect with `nc`, `socat`, `telnet`, or editor plugins like
Conjure and inf-clojure.
Each connection gets an independent read-eval-print loop.

Unlike nREPL (which uses a structured bencode protocol), the socket
REPL is a raw text protocol: send Clojure code, receive printed
results.
No special client library is required.

### `--srepl` Forms

| Form | Meaning |
|------|---------|
| `--srepl` | Start server on localhost, random port |
| `--srepl=7777` | Start server on localhost:7777 |
| `--srepl=0.0.0.0:7777` | Bind to all network interfaces, port 7777 |
| `--srepl=0.0.0.0` | Bind to all interfaces, random port |
| `--srepl=.srepl-port` | Random port, write port number to file |

The value heuristic is the same as `--nrepl`:
all digits = port number,
contains `:` with trailing digits = host:port,
valid IP address = host with random port,
anything else = port file path.

The server prints its address on startup:

```
Socket REPL started on port 7777 on host localhost - localhost:7777
```

Press Ctrl+C to shut down the server.
If a port file was specified, it is removed on shutdown.

### Connecting

Interactive session with readline (via `rlwrap`):

```
rlwrap nc localhost 7777
user=> (+ 1 2)
3
user=> (defn greet [name]
  ..    (str "Hello, " name "!"))
#'user/greet
user=> (greet "world")
"Hello, world!"
```

Piped input:

```
echo '(+ 1 2)' | nc -q 1 localhost 7777
```

### Multiline Input

The socket REPL detects incomplete expressions (unclosed brackets
or strings) and shows a continuation prompt:

```
user=> (defn f [x]
  ..    (+ x 1))
#'user/f
```

### Namespace Tracking

Each connection starts in the `user` namespace.
Switching namespaces updates the prompt:

```
user=> (ns my-app)
nil
my-app=>
```

Each connection has its own namespace state.

### Editor Integration

**Conjure (Neovim):**
Run `gloat --srepl=7777` in a terminal.
Configure Conjure to connect to the socket REPL.

**inf-clojure (Emacs):**
Run `gloat --srepl=7777` in a terminal, then connect with
`M-x inf-clojure-connect`.

### nREPL vs Socket REPL

| | nREPL | Socket REPL |
|---|---|---|
| Protocol | Bencode (structured) | Plain text |
| Clients | Calva, CIDER | nc, Conjure, inf-clojure |
| Features | Eval, completions, docs, interrupt | Eval only |
| Use case | IDE integration | Lightweight debugging, scripting |

## REPL Client Mode

Connect a readline-enabled REPL to a running nREPL server with
`gloat --repl=PORT`.
This gives you the full interactive REPL experience (multiline editing,
tab completion, history) while evaluating code on the remote server.

### `--repl` Forms

| Form | Meaning |
|------|---------|
| `--repl` | Start local interactive REPL |
| `--repl=7888` | Connect to nREPL at localhost:7888 |
| `--repl=host:7888` | Connect to nREPL at host:7888 |
| `--repl=.nrepl-port` | Read port from file, connect |
| `--repl=+bb` | Start Babashka nREPL, connect |
| `--repl=+lein` | Start Leiningen nREPL, connect |
| `--repl=+clj` | Start Clojure CLI nREPL, connect |
| `--repl=+let-go` | Start let-go nREPL, connect |
| `--repl=dir/` | Use `dir` as build directory (trailing slash) |
| `--repl=dir` | Existing directory = build dir; existing file = port file |

The value heuristic:
1. All digits = port number (connect to nREPL)
2. Contains `:` with trailing digits = host:port (connect to nREPL)
3. Starts with `+` = tool name (start tool's nREPL, connect)
4. Existing file = port file (read port, connect)
5. Ends with `/` = build directory (create if needed)
6. Existing directory = build directory
7. Does not exist = error

### External Tool REPL

The `+tool` forms start an external Clojure tool's nREPL server and
connect Gloat's readline REPL to it.
This gives you Gloat's REPL features (syntax highlighting, rainbow
brackets, ghost text, tab completion, vi/emacs modes) while evaluating
code in the external tool's runtime.

```
gloat --repl=+bb      # Babashka
gloat --repl=+lein    # Leiningen (Clojure + project deps)
gloat --repl=+clj     # Clojure CLI
gloat --repl=+let-go  # let-go (Go-based Clojure)
```

Tools are auto-installed if not already present.
The external server is stopped automatically when the REPL exits.

### Piped Input

When stdin is not a terminal, the client evaluates the input and exits
without starting an interactive REPL:

```
gloat --repl=7888 <<<'(+ 1 2)'
# prints: 3

gloat --repl=7888 < script.clj
```

This is useful for scripting against a running server.

### Workflow Example

Terminal 1 -- start the server:
```
gloat --nrepl=7888
```

Terminal 2 -- connect the CLI REPL:
```
gloat --repl=7888
```

Both terminals share the same evaluation state.
An editor (Calva, CIDER) can connect to the same server simultaneously.

## Embedded Servers

Every `gloat --repl` session automatically starts an embedded nREPL
server and an embedded socket REPL server, both on random ports.
This means editors can connect to your REPL session without any
extra setup.

The server URLs are shown in the startup banner:

```
 Glojure: v0.6.4
      Go: 1.24.0 linux/amd64
   nREPL: nrepl://localhost:38291
   sREPL: localhost:35149
    Help: C-h or :repl/help
    Exit: C-d or :repl/exit
```

Use `:repl/server` to display the server URLs at any time.
The URLs also appear in `C-h` help under Current Settings.

The embedded servers do not write port files.
If you need a port file, use `gloat --nrepl=.nrepl-port` or
`gloat --srepl=.srepl-port` instead.

When you exit the REPL, both embedded servers shut down
automatically.

## REPL Build Directory

When launched via `gloat --repl`, the REPL needs a Go module directory
to bootstrap dependencies and generate import glue code.
The build directory is resolved in this priority order:

1. `gloat --repl=dir/` -- explicit directory (trailing slash creates it
   if missing; without the slash the directory must already exist)
2. `GLOAT_REPL=dir` -- environment variable
3. `./gljrepl/` -- auto-selected when `./gljdeps.edn` is present in the
   current directory; the subdirectory keeps generated `go.mod`,
   `go.sum`, and Go glue out of your project root
4. Shared cache (e.g. `~/.cache/gloat/.../repl-VERSION/`) -- fallback
   when no deps file is in sight, so an ad-hoc REPL leaves no files
   behind in the current directory

The build directory is created automatically if it does not exist.
If `./gljdeps.edn` exists, its contents are copied into the build
directory as `gljdeps.edn`; an explicit `--deps=FILE` does the same.

## External Go Dependencies

The Glojure standard library and the Go standard library are available
to every REPL session out of the box.
Third-party Go packages -- anything you would normally `go get` --
become available through a `gljdeps.edn` file.

A minimal `gljdeps.edn` declares the packages you want and the versions
to pin:

```clojure
{:deps {github.com:yaml:go-yaml {:mvn/version "v3.0.1"}}}
```

Each key is the Go import path written as a Glojure symbol, with `/`
rewritten as `:`.
The deps loader rewrites `:` back to `/` before handing the path to
`go get`, so the example above resolves to the module
`github.com/yaml/go-yaml`.
Using the colon form keeps the deps key consistent with how the
package is named at call sites (see [gloat-go-interop](gloat-go-interop.md)).

Multiple packages and pinned versions go in the same map:

```clojure
{:deps {github.com:yaml:go-yaml  {:mvn/version "v3.0.1"}
        github.com:google:uuid   {:mvn/version "v1.6.0"}}}
```

### Loading deps

There are two ways to point the REPL at a deps file.

**File on disk.**
If `./gljdeps.edn` exists, `gloat --repl` picks it up automatically.
Pass `--deps=path/to/file.edn` to use a different name or location.

**Process substitution (one-shot).**
For ad-hoc experiments, feed the deps inline with shell process
substitution:

```bash
gloat --repl --deps=<(echo '{:deps {github.com:yaml:go-yaml {:mvn/version "v3.0.1"}}}')
```

On first launch, gloat fetches the declared packages with `go get`,
generates the Go import glue, and re-execs the REPL with the new
packages linked in.
Subsequent launches with the same deps skip the fetch.
First-run latency is a few seconds; reopens are instantaneous.

### Worked example

Launch a REPL with `github.com/yaml/go-yaml` available:

```
$ gloat --repl --deps=<(echo '{:deps {github.com:yaml:go-yaml {:mvn/version "v3.0.1"}}}')
🐐 Gloat: 0.1.40 🐐
 Glojure: v0.6.4
      Go: 1.24.0 linux/amd64
   nREPL: nrepl://localhost:38291
   sREPL: localhost:35149
    Help: C-h or :repl/help
    Exit: C-d or :repl/exit

user=> (def m (go/make (go/map-of go/string go/string)))
#'user/m
user=> (.SetMapIndex (reflect.ValueOf m)
                     (reflect.ValueOf "greeting") (reflect.ValueOf "hello"))
nil
user=> (let [[bs err] (github.com:yaml:go-yaml.Marshal m)]
         (when err (throw err))
         (print (go/string bs)))
greeting: hello
nil
```

A Glojure map literal does not yet auto-convert to a Go `map[K]V`, so
the example builds the map with `go/make` and writes entries through
`reflect.ValueOf` before passing it to `Marshal`.
`Marshal` returns `([]byte, error)`, which destructures as a vector;
`(go/string bs)` casts the bytes back to a Glojure string for display.

### Cross-reference

For the interop forms used above (`pkg.Func`, `.Method`, `(new ...)`,
destructuring multi-return), see
[gloat-go-interop](gloat-go-interop.md).

## Inline Documentation

Press **Ctrl+D** (vi mode) or **Ctrl+X Ctrl+D** (emacs mode) with the
cursor on or inside a symbol to display its documentation below the
input line.
The hint shows the ClojureDocs URL (for `clojure.*` symbols), the
fully qualified name, arglists, and docstring.

```
user=> (zero? 42)
https://clojuredocs.org/clojure.core/zero_q
clojure.core/zero?
([num])
  Returns true if num is zero, else false
```

The documentation appears as a transient hint that disappears on the
next keypress.
Press **Escape** to dismiss the hint without leaving insert mode.
The cursor can be anywhere on the symbol (not just at the end).

If the cursor is on a number, bracket, or whitespace, nothing is shown.
On an empty prompt, Ctrl+D exits the REPL as usual.

## Inline Help

Press **Ctrl+H** (vi mode) or **Ctrl+X Ctrl+H** (emacs mode) to
display a quick-reference card showing key bindings and REPL commands
below the input line.
The help content adapts to the current editing mode, showing the
appropriate key bindings.
Like inline documentation, the help hint is transient and disappears
on the next keypress.
Press **Escape** to dismiss it without leaving insert mode.

## Format and Print

Press **Ctrl+P** (vi mode) or **Ctrl+X Ctrl+P** (emacs mode) to
format the current form, print it as plain text, copy it to the
system clipboard, save it to history, and start a fresh prompt.

The form is piped through an external format command before printing
and copying.
The default format command is `cat` (no formatting).
To use a Clojure formatter like `zprint`:

```
:repl/fmt zprint
```

The format command can also be set via the `GLJ_REPL_FORMATTER`
environment variable.
The command is run with `sh -c`, so arguments are supported (e.g.
`:repl/fmt zprint '{:width 40}'`).

Type `:repl/fmt` with no argument to show the current format command.

## Share

Press **Ctrl+S** to print a browser REPL share URL and copy the
shared forms to the system clipboard as plain text.
When browsing history, the selected history form and every newer form
are shared.
At the end of history, the current input is shared if non-empty;
otherwise the last history form is shared.

## REPL Commands

The following colon-commands can be typed at the prompt.
They support tab completion -- type `:repl/` and press Tab to see all
available commands.

| Command | Action |
|---------|--------|
| `:repl/help` | Print key bindings and commands (mode-aware) |
| `:repl/vi` | Switch to vi editing mode |
| `:repl/emacs` | Switch to emacs editing mode |
| `:repl/fmt cmd` | Set the format command (used by Ctrl+P) |
| `:repl/fmt` | Show the current format command |
| `:repl/server` | Show the nREPL server URL |
| `:repl/exit` | Exit the REPL |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `GLJ_REPL_EDITOR` | Set editing mode (`vi` or `emacs`). Overrides `~/.inputrc`. |
| `GLJ_REPL_FORMATTER` | Format command for Ctrl+P (default: `cat`). |
| `GLJ_REPL_NO_BANNER` | Suppress the startup banner when set. |
| `GLOAT_GLJDEPS` | Path to a `gljdeps.edn` file (alternative to `--deps=FILE`). |
| `GLOAT_REPL` | Set the REPL build directory. |
| `GLOAT_REPL_HISTORY_BB` | Babashka history file for `--repl=+bb` (JLine format). |
| `GLOAT_REPL_HISTORY_LEIN` | Leiningen history file for `--repl=+lein` (JLine format). |
| `GLOAT_REPL_HISTORY_CLJ` | Clojure CLI history file for `--repl=+clj` (JLine format). |
| `GLOAT_REPL_HISTORY_LET_GO` | let-go history file for `--repl=+let-go` (JLine format). |
| `GLOAT_REPL_HISTORY_LG` | let-go history file for `--repl=+lg` (JLine format). |
| `INPUTRC` | Path to inputrc config file (default: `~/.inputrc`). |

## Feature Comparison

How the Gloat/Glojure REPL compares to other Clojure REPLs.

| Feature | gloat --repl | bb (1.12+) | lein repl | clj |
|---------|-----|------------|-----------|-----|
| Syntax highlighting | **✓** | **✗** | **✗** | **✗** |
| Rainbow brackets | **✓** | **✗** | **✗** | **✗** |
| Format and clipboard (Ctrl+P) | **✓** | **✗** | **✗** | **✗** |
| Share URL (Ctrl+S) | **✓** | **✗** | **✗** | **✗** |
| Multiline editing | **✓** | **✓** | **✗** | **✗** |
| Tab completion (syms & keys) | **✓** | **✓** | **✓** | **✗** |
| Smart up/down arrow navigation | **✓** | **✗** | **✗** | **✗** |
| Smart indent/dedent (Tab/Backspace) | **✓** | **✗** | **✗** | **✗** |
| Auto-indent | **✓** | **✗** | **✗** | **✗** |
| Auto-suggest (ghost text) | **✓** | **✓** | **✗** | **✗** |
| Instant docs at cursor (Ctrl+D) | **✓** | **✓** | **✗** | **✗** |
| Completion descriptions | **✓** | **✗** | **✗** | **✗** |
| Persistent history | **✓** | **✓** | **✓** | **✗** |
| Reverse search (Ctrl+R) | **✓** | **✓** | **✓** | **✓** |
| Suspend/resume (Ctrl+Z) | **✓** | **✓** | **✓** | **✓** |
| Interrupt evaluation (Ctrl+C) | **✓** | **✓** | **✓** | **✓** |
| Vi editing mode | **✓** | **✗** | **✗** | **✗** |
| Emacs editing mode | **✓** | **✓** | **✓** | **✗** |
| Runtime mode switching | **✓** | **✗** | **✗** | **✗** |

**Notes:**

- **clj** refers to the bare `clojure` CLI REPL without third-party
  tools.
  With rebel-readline, clj gains completion, multiline editing, and
  syntax highlighting.
- **bb** refers to Babashka v1.12+ which uses JLine3 for its console
  REPL.
- **lein repl** uses REPL-y (JLine-based) for its console interface.

## Web REPL

The Gloat website includes a browser-based REPL powered by Glojure
compiled to WebAssembly.
It provides a subset of the CLI REPL's features and requires no
installation.

### How It Works

The web REPL loads a Glojure Wasm binary (`glj.wasm`) and runs it
inside the browser.
User input is fed to the Wasm runtime through a hooked `stdin`, and
evaluation output is captured from `stdout`/`stderr` and displayed
in the terminal pane.

The REPL initializes automatically when the page loads.
A loading spinner is shown while the Wasm binary downloads and
initializes.

### Supported Features

The web REPL supports the following features:

- **Syntax highlighting** -- the same token types as the CLI REPL
  (strings, keywords, comments, numbers, booleans, special forms,
  core functions) are highlighted as you type.
- **Rainbow brackets** -- brackets are colored by nesting depth
  using the same Calva-style color scheme as the CLI REPL.
  Mismatched and unmatched brackets are shown with white text on a
  red background.
- **Multiline input** -- incomplete expressions (unclosed brackets
  or strings) automatically continue on the next line with
  indentation matching the nesting depth.
- **History** -- Up/Down arrows (or Ctrl+P/Ctrl+N) navigate through
  previous inputs within the session.
- **Smart indentation** -- Tab inserts two spaces.
  Backspace deletes two spaces at once when preceded by two spaces,
  matching the CLI's indent-aware behavior.
- **Editing shortcuts** -- Ctrl+A (beginning of line), Ctrl+E (end
  of line), Ctrl+U (kill before cursor), Ctrl+K (kill after cursor),
  Ctrl+L (clear screen).
- **Paste support** -- pasted text is inserted as plain text with
  carriage returns normalized and trailing newlines stripped.

### Toolbar

The toolbar above the terminal provides quick-access buttons:

| Button | Action |
|--------|--------|
| Share | Copy a shareable URL (with input state) to clipboard |
| Copy | Copy the current form (or last history entry) to clipboard |
| Up/Down | Navigate history |
| Kill left/right | Delete before/after cursor |
| Clear | Clear the terminal output |

### Sharing

The web REPL supports sharing expressions via URL fragments.
When you type or evaluate expressions, the URL hash is updated with
a base64-encoded snapshot of your history.
Sharing this URL loads the REPL with the same expressions
pre-evaluated, and the last expression placed in the input field
ready to run.
If the history cursor is at the end and the input is empty, sharing
uses the last history form.

### Limitations

The web REPL does not support:

- **Tab completion** -- there is no completion menu; Tab inserts
  spaces.
- **Ghost text** (autosuggestions).
- **Vi/Emacs editing modes** -- only basic editing is available.
- **Persistent history** -- history is lost when the page is
  reloaded.
- **Ctrl+D documentation lookup**.
- **nREPL server** -- the Wasm runtime runs entirely in the browser.
- **External Go dependencies** -- only the built-in standard library
  and clojure.core are available.
- **Suspend/resume** (Ctrl+Z).

### CLI vs Web REPL Feature Matrix

| Feature | CLI | Web |
|---------|-----|-----|
| Syntax highlighting | Yes | Yes |
| Rainbow brackets | Yes | Yes |
| Multiline editing | Yes | Yes |
| Smart indent/dedent | Yes | Yes |
| History navigation | Yes | Yes (session only) |
| Paste support | Yes | Yes |
| Shareable URLs | No | Yes |
| Tab completion | Yes | No |
| Ghost text | Yes | No |
| Vi/Emacs modes | Yes | No |
| Persistent history | Yes | No |
| Inline docs (Ctrl+D) | Yes | No |
| nREPL server | Yes | No |
| External Go deps | Yes | No |

## See Also

**gloat**(1)
