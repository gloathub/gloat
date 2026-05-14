# gloat-install -- Installing Gloat

Gloat has **zero dependencies** - everything installs automatically on first
use.

There are two main ways to get started: cloning the repository and sourcing
the `.rc` file (recommended), or the one-line installer.


## Clone and Setup (Recommended)

For Bash, Fish or Zsh:

```bash
git clone https://github.com/gloathub/gloat
source gloat/.rc
gloat --help
```

The `source gloat/.rc` command adds the `gloat` command to your PATH, enables
the `man gloat` help and sets up `gloat` tab completion.

On first run, Gloat will automatically install all required tools (Go, Glojure,
YAMLScript, Babashka, etc) to `.cache/local/` within the project directory.
Just run `gloat --help` once to complete the setup.

To make Gloat available permanently, simply add this to your shell's rc file
(`~/.bashrc`, `~/.config/fish/config.fish` or `~/.zshrc`):

```bash
source /path/to/gloat/.rc
```

Yes, that command and `.rc` file actually supports all three shells!


## One-Line Installer

Install `gloat` (or `glj`) to `~/.local/bin/` with a single command:

```bash
make -f <(curl -sL gloathub.org/make) install
```

Installing `gloat` clones the gloat repository to `~/.local/share/gloat`,
creates a symlink at `~/.local/bin/gloat`, and installs all the required
dependencies automatically.

The installer refuses to run if `~/.local/bin` is not on your `PATH` —
add `export PATH=~/.local/bin:$PATH` to your shell rc and start a new
shell, or pass `PREFIX=/path/to` where `/path/to/bin` is already on your
`PATH`. Then run `gloat --help`.

**Options:**

```bash
# Install to a custom prefix
make -f <(curl -sL gloathub.org/make) install PREFIX=~/.gloat

# Install a specific version
make -f <(curl -sL gloathub.org/make) install VERSION=v0.1.37

# Also install the Glojure glj (prebuilt binary) command
make -f <(curl -sL gloathub.org/make) install-glj

# Uninstall
make -f <(curl -sL gloathub.org/make) uninstall
make -f <(curl -sL gloathub.org/make) uninstall-glj

# Show the installer Makefile rules:
make -f <(curl -sL gloathub.org/make) help
```

On success the installer prints the exact `source .../share/gloat/.rc`
line to add to your shell rc — this gives the one-line install the same
man-page support and shell completion as the clone method.

Pass `VERSION=v1.2.3` to pin to a specific gloat release; the installer
also picks up the GLJ version that pairs with that gloat tag.


## Upgrading

Upgrade gloat to the latest release:

```bash
gloat --upgrade
```

Pin (or roll back) to a specific version:

```bash
gloat --upgrade=v1.2.3
```

In both cases, gloat will:

1. Switch the local checkout to the target tag.
2. Remove `.cache/` so all dependencies (including the GLJ version paired
   with that gloat release) reinstall fresh on the next run.
3. Print what to expect — the next `gloat` invocation kicks off a quiet
   per-tool install.

If your clone has uncommitted changes, `--upgrade` will refuse and ask you
to stash or commit them first.

If you installed via the one-line installer, the checkout under
`~/.local/share/gloat` is detached-HEAD and upgrading there always works.


## Resetting the Cache

If something gets stuck or you want to force a fresh dependency install
without changing version:

```bash
gloat --reset
```

This removes `.cache/` entirely (binaries, build artifacts, REPL working
dirs). The next `gloat` invocation reinstalls everything.


## Uninstalling

If you used the one-line installer:

```bash
make -f <(curl -sL gloathub.org/make) uninstall
make -f <(curl -sL gloathub.org/make) uninstall-glj   # if you installed glj
```

This removes the `gloat` symlink from `~/.local/bin` and the cloned tree
from `~/.local/share/gloat` (use `PREFIX=...` if you installed to a custom
prefix). After uninstalling, remove the `source .../.rc` line you added to
your shell rc, or new shells will fail to start.

If you used the clone method, uninstalling is just removing the clone and
the `source` line:

```bash
rm -rf /path/to/gloat
# Then remove the `source /path/to/gloat/.rc` line from your shell rc.
```
