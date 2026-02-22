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

# Configure glojure to use gloat's custom fork
GLOJURE-VERSION := 0.6.5-rc6
GLOJURE-REPO := https://github.com/gloathub/glojure

include $M/glojure.mk

# Installation prefix (default: ~/.local)
PREFIX ?= $(HOME)/.local

# Git repository URL
GLOAT-REPO ?= https://github.com/gloathub/gloat

# Version to install (default: latest tag)
VERSION ?= $(shell \
  git ls-remote --tags --sort=-v:refname $(GLOAT-REPO) | \
	grep -v '\^{}' | head -n1 | sed 's/.*refs\/tags\///')

# Installation paths
INSTALL-DIR := $(PREFIX)/share/gloat
BIN-DIR := $(PREFIX)/bin
GLOAT-BIN := $(BIN-DIR)/gloat

help:
	@echo "Gloat Installer Makefile targets:"
	@echo ""
	@echo "  install          Alias for install-gloat"
	@echo "  install-gloat    Install gloat"
	@echo "  install-glj      Install glj"
	@echo "  uninstall        Alias for uninstall-gloat"
	@echo "  uninstall-gloat  Uninstall gloat"
	@echo "  uninstall-glj    Uninstall glj"
	@echo ""
	@echo "Options:"
	@echo "  PREFIX=<path>    Installation prefix (default: ~/.local)"
	@echo "  VERSION=<tag>    Version to install (default: latest tag)"

install:
	@echo "Installing Gloat to $(PREFIX)..."
	@# Create directories
	@mkdir -p "$(BIN-DIR)" "$(PREFIX)/share"
	@# Clone repository
	@if [ -d "$(INSTALL-DIR)" ]; then \
		echo "Updating existing installation at $(INSTALL-DIR)..."; \
		cd "$(INSTALL-DIR)" && git fetch --tags; \
	else \
		echo "Cloning Gloat from $(GLOAT-REPO)..."; \
		git clone "$(GLOAT-REPO)" "$(INSTALL-DIR)"; \
	fi
	@# Checkout version
	@cd "$(INSTALL-DIR)" && git -c advice.detachedHead=false checkout "$(VERSION)"
	@# Create symlink
	@ln -sf "$(INSTALL-DIR)/bin/gloat" "$(GLOAT-BIN)"
	@# Install dependencies
	@echo "Installing gloat dependencies..."
	"$(GLOAT-BIN)" --version </dev/null || true
	@echo ""
	@echo "✓ Gloat $(VERSION) installed successfully!"
	@echo ""
	@echo "  Installed to: $(INSTALL-DIR)"
	@echo "  Symlink at:   $(GLOAT-BIN)"
	@echo ""
	@# Check if PREFIX/bin is in PATH
	@if ! echo "$$PATH" | grep -q "$(BIN-DIR)"; then \
		echo "⚠ Add $(BIN-DIR) to your PATH:"; \
		echo ""; \
		echo "  export PATH=$(BIN-DIR):\$$PATH"; \
		echo ""; \
	fi
	@echo "Run 'gloat --help' to get started."

uninstall: uninstall-gloat

uninstall-gloat:
	@echo "Uninstalling Gloat from $(PREFIX)..."
	@# Remove symlink
	@if [ -L "$(GLOAT-BIN)" ]; then \
		rm -f "$(GLOAT-BIN)"; \
		echo "✓ Removed symlink: $(GLOAT-BIN)"; \
	fi
	@# Remove installation directory
	@if [ -d "$(INSTALL-DIR)" ]; then \
		rm -rf "$(INSTALL-DIR)"; \
		echo "✓ Removed directory: $(INSTALL-DIR)"; \
	fi
	@echo ""
	@echo "✓ Gloat uninstalled successfully!"

uninstall-glj:
	@echo "Uninstalling glj from $(PREFIX)..."
	@if [ -f "$(BIN-DIR)/glj" ]; then \
		rm -f "$(BIN-DIR)/glj"; \
		echo "✓ Removed: $(BIN-DIR)/glj"; \
	fi
	@echo ""
	@echo "✓ glj uninstalled successfully!"

install-gloat: install

install-glj: $(GLJ)
	@echo "Installing glj to $(PREFIX)..."
	@mkdir -p "$(BIN-DIR)"
	@cp "$(GLJ)" "$(BIN-DIR)/glj"
	@echo ""
	@echo "✓ glj installed successfully!"
	@echo ""
	@echo "  Installed to: $(BIN-DIR)/glj"
	@echo ""
	@# Check if PREFIX/bin is in PATH
	@if ! echo "$$PATH" | grep -q "$(BIN-DIR)"; then \
		echo "⚠ Add $(BIN-DIR) to your PATH:"; \
		echo ""; \
		echo "  export PATH=$(BIN-DIR):\$$PATH"; \
		echo ""; \
	fi
	@echo "Run 'glj --help' to get started."
