M := ../.cache/makes
$(shell [ -d $M ] || (git clone -q https://github.com/makeplus/makes $M))
include $M/init.mk
include $M/clean.mk
PYTHON-VENV := venv
include $M/python.mk
include $M/typos.mk
SHELL-DEPS += $(PYTHON-VENV)
include $M/shell.mk

PYTHON-VENV-SETUP := pip install -r requirements.txt

REPO ?= git@github.com:gloathub/gloat

MAKES-CLEAN := site
MAKES-REALCLEAN := $(PYTHON-VENV)
MAKES-DISTCLEAN := .cache/

DEPS := \
  $(PYTHON-VENV) \


default::

# Build main site (production)
site: $(DEPS)
	mkdocs build -d $@

# Serve locally with MkDocs
serve: $(DEPS)
	mkdocs serve --livereload

# Build alias for backwards compatibility
build: site

# Lint check
lint: $(TYPOS)
	typos

# Deploy to production
publish: site
	cd $< && \
	  git init && \
	  git add -A && \
	  git commit -m 'Deploy to production' && \
	  git push -f $(REPO) HEAD:gh-pages
	$(RM) -r $<
