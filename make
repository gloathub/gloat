# Gloat Installer Makefile
#
# Usage:
#   make -f <(curl -sL gloathub.org/make) install
#   make -f <(curl -sL gloathub.org/make) install PREFIX=~/.mydir
#   make -f <(curl -sL gloathub.org/make) install-gloat
#   make -f <(curl -sL gloathub.org/make) install-glj
#   make -f <(curl -sL gloathub.org/make) uninstall
#   make -f <(curl -sL gloathub.org/make) uninstall-gloat
#   make -f <(curl -sL gloathub.org/make) uninstall-glj

# Clone makes to temporary directory
T := $(shell mktemp -d)
M := $T/makes
$(shell git clone -q https://github.com/makeplus/makes $M)
include $M/init.mk

# Installation prefix (default: ~/.local)
PREFIX ?= $(HOME)/.local

# Git repository URL
GLOAT-REPO ?= https://github.com/gloathub/gloat

# Version to install (default: latest tag). If user passes VERSION=, skip
# the remote tag lookup.
ifeq ($(origin VERSION),undefined)
VERSION := $(shell \
  git ls-remote --tags --sort=-v:refname $(GLOAT-REPO) 'refs/tags/v*' | \
    grep -v '\^{}' | head -n1 | sed 's/.*refs\/tags\///')
endif
override VERSION := $(if $(filter v%,$(VERSION)),$(VERSION),v$(VERSION))

# Installation paths
INSTALL-DIR := $(PREFIX)/share/gloat
BIN-DIR := $(PREFIX)/bin
GLOAT-BIN := $(BIN-DIR)/gloat

define PATH_ERROR

Error: '$(BIN-DIR)' is not on your PATH.

You have two options:

  1. Add '$(BIN-DIR)' to your PATH. Put this line in your shell rc
     (~/.bashrc, ~/.zshrc, or ~/.config/fish/config.fish),
     start a new shell, then retry the install:

         export PATH=$(BIN-DIR):$$PATH

  2. Re-run the install with PREFIX set to a directory whose 'bin/'
     is already on your PATH.
     For example, if '/path/to/bin' is on your PATH:

         make -f <(curl -sL gloathub.org/make) install PREFIX=/path/to

endef
export PATH_ERROR

help:
	@echo 'Gloat Installer Makefile targets:'
	@echo ''
	@echo '  install          Alias for install-gloat'
	@echo '  install-gloat    Install gloat'
	@echo '  install-glj      Install glj'
	@echo '  uninstall        Alias for uninstall-gloat'
	@echo '  uninstall-gloat  Uninstall gloat'
	@echo '  uninstall-glj    Uninstall glj'
	@echo ''
	@echo 'Options:'
	@echo '  PREFIX=<path>    Installation prefix (default: ~/.local)'
	@echo '  VERSION=<tag>    Version to install (default: latest tag)'

# User might have a ./install file or dir, so use PHONY here.
.PHONY: help install install-glj install-gloat uninstall uninstall-glj uninstall-gloat
install:
	@# PATH check up front — refuse to install if BIN-DIR isn't on PATH so
	@# the user doesn't end up with a 'gloat' they can't run.
	@if ! echo ":$$PATH:" | grep -q ':$(BIN-DIR):'; then \
	  echo "$$PATH_ERROR" >&2; \
	  exit 1; \
	fi
	@echo 'Installing Gloat to $(PREFIX)...'
	@mkdir -p '$(BIN-DIR)' '$(PREFIX)/share'
	@# Clone repository
	@if [[ -d '$(INSTALL-DIR)' ]]; then \
	  echo 'Updating existing installation at $(INSTALL-DIR)...'; \
	  cd '$(INSTALL-DIR)' && git fetch --tags --quiet; \
	else \
	  echo 'Cloning Gloat from $(GLOAT-REPO)...'; \
	  git clone --quiet '$(GLOAT-REPO)' '$(INSTALL-DIR)'; \
	fi
	@# Validate the requested version exists as a tag
	@cd '$(INSTALL-DIR)' && \
	  git rev-parse --verify --quiet '$(VERSION)^{commit}' >/dev/null || { \
	    echo "Error: '$(VERSION)' is not a known gloat tag." >&2; \
	    echo "Recent tags:" >&2; \
	    cd '$(INSTALL-DIR)' && \
	      git tag -l 'v*' --sort=-v:refname | head -5 | sed 's/^/  /' >&2; \
	    exit 1; \
	  }
	@# Checkout version
	@cd '$(INSTALL-DIR)' && \
	  git -c advice.detachedHead=false checkout --quiet '$(VERSION)'
	@# Clear cache so the correct paired dependency versions install
	@chmod -R u+w '$(INSTALL-DIR)/.cache' 2>/dev/null || true
	@rm -rf '$(INSTALL-DIR)/.cache'
	@# Create symlink
	@ln -sf '$(INSTALL-DIR)/bin/gloat' '$(GLOAT-BIN)'
	@# Per-tool quiet deps install, driven from this Makefile (independent
	@# of which bin/gloat version was cloned). Captures each tool's output
	@# and only dumps it on failure.
	@echo ''
	@echo 'Installing gloat dependencies into $(INSTALL-DIR)/.cache/local/'
	@for tool in bb go glojure glj ys; do \
	  printf '==> Installing %s... ' "$$tool"; \
	  log=$$(mktemp); \
	  if $(MAKE) --quiet --no-print-directory -C '$(INSTALL-DIR)' \
	      "path-$$tool" > "$$log" 2>&1; then \
	    echo 'done'; \
	    rm -f "$$log"; \
	  else \
	    rc=$$?; \
	    echo 'FAILED'; \
	    echo ''; \
	    cat "$$log"; \
	    rm -f "$$log"; \
	    exit "$$rc"; \
	  fi; \
	done
	@echo ''
	@echo '✓ Gloat $(VERSION) installed successfully!'
	@echo ''
	@echo '  Installed to: $(INSTALL-DIR)'
	@echo '  Symlink at:   $(GLOAT-BIN)'
	@echo ''
	@# Shell-specific rc hint for man pages and completion
	@case $$(basename "$${SHELL:-}") in \
	  bash) echo 'To enable tab completion and gloat* man pages, run this command:'; \
	        echo '  source $(INSTALL-DIR)/.rc'; \
	        echo '(or add it to your ~/.bashrc file to make it permanent)'; \
	        echo 'See: https://gloathub.org/doc/gloat-install/' ;; \
	  fish) echo 'To enable tab completion and gloat* man pages, run this command:'; \
	        echo '  source $(INSTALL-DIR)/.rc'; \
	        echo '(or add it to your ~/.config/fish/config.fish file to make it permanent)'; \
	        echo 'See: https://gloathub.org/doc/gloat-install/' ;; \
	  zsh)  echo 'To enable tab completion and gloat* man pages, run this command:'; \
	        echo '  source $(INSTALL-DIR)/.rc'; \
	        echo '(or add it to your ~/.zshrc file to make it permanent)'; \
	        echo 'See: https://gloathub.org/doc/gloat-install/' ;; \
	esac
	@echo ''
	@echo "Run 'gloat --help' to get started."

uninstall: uninstall-gloat

uninstall-gloat:
	@if [[ ! -L '$(GLOAT-BIN)' && ! -d '$(INSTALL-DIR)' ]]; then \
	  echo "Gloat is not installed at '$(PREFIX)'." >&2; \
	  echo "(No symlink at '$(GLOAT-BIN)' and no directory at '$(INSTALL-DIR)'.)" >&2; \
	  exit 1; \
	fi
	@echo 'Uninstalling Gloat from $(PREFIX)...'
	@if [[ -L '$(GLOAT-BIN)' ]]; then \
	  rm -f '$(GLOAT-BIN)'; \
	  echo '✓ Removed symlink: $(GLOAT-BIN)'; \
	fi
	@if [[ -d '$(INSTALL-DIR)' ]]; then \
	  chmod -R +w '$(INSTALL-DIR)'; \
	  rm -rf '$(INSTALL-DIR)'; \
	  echo '✓ Removed directory: $(INSTALL-DIR)'; \
	fi
	@echo ''
	@echo '✓ Gloat uninstalled successfully!'
	@echo ''
	@echo 'If you added a "source $(INSTALL-DIR)/.rc" line to your shell rc'
	@echo '(~/.bashrc, ~/.zshrc, or ~/.config/fish/config.fish), remove it'
	@echo 'now or new shells will fail to start.'

uninstall-glj:
	@if [[ ! -f '$(BIN-DIR)/glj' ]]; then \
	  echo "glj is not installed at '$(BIN-DIR)/glj'." >&2; \
	  exit 1; \
	fi
	@echo 'Uninstalling glj from $(PREFIX)...'
	@rm -f '$(BIN-DIR)/glj'
	@echo '✓ Removed: $(BIN-DIR)/glj'
	@echo ''
	@echo '✓ glj uninstalled successfully!'

install-gloat: install

# install-glj reuses install (which clones + checks out the right tag),
# then copies the glj that the cloned gloat checkout considers correct
# (i.e., the GLOJURE-VERSION pinned in that tag's common/common.mk).
install-glj: install
	@echo 'Locating paired glj for gloat $(VERSION)...'
	@glj_path=$$('$(GLOAT-BIN)' --glj) && \
	  cp "$$glj_path" '$(BIN-DIR)/glj'
	@echo ''
	@echo '✓ glj (paired with $(VERSION)) installed to $(BIN-DIR)/glj'
	@if ! echo '$$PATH' | grep -q '$(BIN-DIR)'; then \
	  echo ''; \
	  echo '⚠ Add $(BIN-DIR) to your PATH:'; \
	  echo '  export PATH=$(BIN-DIR):\$$PATH'; \
	fi
	@echo ''
	@echo "Run 'glj --help' to get started."
