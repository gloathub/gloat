# gloat-install -- Installing Gloat

Gloat has **zero dependencies** - everything installs automatically on first
use.

There are two main ways to get started: the one-line installer or cloning the
repository and sourcing the `.rc` file.


## One-Line Installer

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
make -f <(curl -sL gloathub.org/make) install VERSION=v0.1.37

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


## Clone and Setup

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
(`~/.bashrc`, `~/.fishrc` or `~/.zshrc`):

```bash
source /path/to/gloat/.rc
```

Yes, that command and `.rc` file actually supports all three shells!
