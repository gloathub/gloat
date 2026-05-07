# gloat-repl -- Gloat/Glojure REPL Reference

## Synopsis

```
gloat --repl [=dir]
glj
```

## Description

The Gloat/Glojure REPL is a rich interactive environment for evaluating
Clojure expressions with full access to Go packages.
It is built on the reeflective/readline library and supports vi and
emacs editing modes, multiline editing with auto-indent, tab completion
with namespace-aware descriptions, ghost text autosuggestions,
persistent history, job control, and bracketed paste.

## Startup

On launch the REPL prints a banner with version and platform
information and quick-reference hints:

```
🐐 Gloat: 0.1.30 🐐
 Glojure: v0.6.5-rc17
      Go: 1.24.0 linux/amd64
    Help: C-h or :repl/help
    Exit: C-d or :repl/exit

user=>
```

When launched with `--deps`, the banner also shows the build
directory.

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
| Brackets | `()[]{}` | Default |
| Symbols | everything else | Default |

Special forms include `def`, `defn`, `defmacro`, `fn`, `let`, `loop`,
`recur`, `if`, `when`, `cond`, `do`, `ns`, `require`, `import`,
`try`, `catch`, `throw`, and others.

Highlighting works correctly with multiline editing, ghost text, and
tab completion.

## REPL Build Directory

When launched via `gloat --repl`, the REPL needs a Go module directory
to bootstrap dependencies and generate import glue code.
The build directory is resolved in this priority order:

1. `--repl=dir` -- explicit directory
2. `GLOAT_REPL=dir` -- environment variable
3. `.` (current directory) -- when `./gljdeps.edn` is present
4. `./gljrepl/` -- default fallback (created automatically)

## External Go Dependencies

To make Go packages available in the REPL, declare them in a
`gljdeps.edn` file:

```clojure
{:deps {gopkg.in/yaml.v3      {:mvn/version "v3.0.1"}
        github.com/some/other  {:mvn/version "v1.2.3"}}}
```

On first run, `glj` fetches the declared packages, generates import
glue code, and re-launches with the new packages available.
Subsequent runs skip the fetch if versions match.

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
| `:repl/exit` | Exit the REPL |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `GLJ_REPL_EDITOR` | Set editing mode (`vi` or `emacs`). Overrides `~/.inputrc`. |
| `GLJ_REPL_FORMATTER` | Format command for Ctrl+P (default: `cat`). |
| `GLJ_REPL_NO_BANNER` | Suppress the startup banner when set. |
| `GLOAT_REPL` | Set the REPL build directory. |
| `INPUTRC` | Path to inputrc config file (default: `~/.inputrc`). |

## Feature Comparison

How the Gloat/Glojure REPL compares to other Clojure REPLs.

| Feature | gloat --repl | bb (1.12+) | lein repl | clj |
|---------|-----|------------|-----------|-----|
| Syntax highlighting | **✓** | **✗** | **✗** | **✗** |
| Format and clipboard (Ctrl+P) | **✓** | **✗** | **✗** | **✗** |
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

## See Also

**gloat**(1)
