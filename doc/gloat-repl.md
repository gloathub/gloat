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
information, followed by quick-reference hints:

```
Glojure v0.6.5-rc17
Go 1.24.0 linux/amd64
    Docs: (doc function-name)
  Source: (source function-name)
    Exit: Ctrl+D or :repl/exit

user=>
```

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

## Multiline Editing

The REPL automatically detects incomplete expressions.
If you press Enter on an unfinished form (unclosed parentheses, missing
string delimiter, etc.), a continuation prompt appears:

```
user=> (defn greet [name]
...   (str "Hello, " name "!"))
#'user/greet
```

When the cursor is not at the end of the buffer, Enter always inserts a
newline regardless of whether the expression is complete.
This lets you go back and add lines in the middle of a multiline form.

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
| **Enter** | Submit expression (cursor at end and expression complete) or insert newline |
| **Ctrl+C** | Cancel current input; on empty prompt, shows exit hint; press twice to exit |
| **Ctrl+D** | Exit the REPL (EOF) |
| **Ctrl+E** | Move cursor to end of line (vi-insert and emacs modes) |
| **Ctrl+R** | Reverse incremental history search |
| **Ctrl+Z** | Suspend the REPL process; resume with `fg` |
| **Up/Down** | Navigate history or move between lines in multiline input |
| **Escape** | Switch to vi-command mode |

## History

Command history is saved to `~/.glj_history` and persists across
sessions.
Use the Up and Down arrow keys (or `k` and `j` in vi-command mode) to
navigate previous entries.
Press **Ctrl+R** to start a reverse incremental search -- type a
substring and the most recent matching entry is recalled.

History entries can span multiple lines.

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

- **Ctrl+D** -- sends EOF, exits immediately.
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

## Feature Comparison

How the Gloat/Glojure REPL compares to other Clojure REPLs.

| Feature | gloat --repl | bb (1.12+) | lein repl | clj |
|---------|-----|------------|-----------|-----|
| Syntax highlighting | **✓** | **✗** | ✗ | **✗** |
| Multiline editing | **✓** | ✓ | **✗** | ✗ |
| Tab completion (symbols) | **✓** | ✓ | **✓** | **✗** |
| Tab completion (keywords) | **✓** | ✓ | **✓** | **✗** |
| Ghost text (autosuggestions) | **✓** | ✓ | **✗** | ✗ |
| Auto-indent | **✓** | **✗** | ✗ | **✗** |
| Smart indent/dedent (Tab/Backspace) | **✓** | **✗** | ✗ | **✗** |
| Completion descriptions | **✓** | **✗** | ✗ | **✗** |
| Persistent history | **✓** | ✓ | **✓** | **✗** |
| Reverse search (Ctrl+R) | **✓** | ✓ | **✓** | ✓ |
| Suspend/resume (Ctrl+Z) | **✓** | ✓ | **✓** | ✓ |
| Interrupt evaluation (Ctrl+C) | **✓** | ✓ | **✓** | ✓ |
| Emacs editing mode | **✓** | ✓ | **✓** | **✗** |
| Vi editing mode | **✓** | **✗** | ✗ | **✗** |

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
