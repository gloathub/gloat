M := ../.cache/makes
$(shell [ -d $M ] || (git clone -q https://github.com/makeplus/makes $M))
include $M/init.mk
include $M/clean.mk
PYTHON-VENV := venv
include $M/python.mk
include $M/typos.mk
SHELL-DEPS += $(PYTHON-VENV)
include $M/shell.mk

export UV_LINK_MODE := copy
export NO_MKDOCS_2_WARNING := 1

# Override python.mk's venv recipe: use uv venv instead of python -m venv
# (avoids ensurepip hang with uv-managed standalone Python builds).
# TODO: Fix python.mk to support recipe override without Make warning.
$(PYTHON-VENV): $(PYTHON)
	@echo '+++ Installing a Python virtualenv in $@'
	uv venv --python $(PYTHON-NAME) $@
	$(if $(wildcard requirements.txt),uv pip install -r requirements.txt,true)
	@echo

REPO ?= git@github.com:gloathub/gloat

MAKES-CLEAN := \
  site \
  docs/build-report \
  docs/demo-assets \
  build-report \

MAKES-REALCLEAN := $(PYTHON-VENV)
MAKES-DISTCLEAN := .cache/

DEPS := \
  $(PYTHON-VENV) \


DEMO-PROGRAMS := \
  99-bottles-of-beer \
  dragon-curve \
  fizzbuzz \
  floyds-triangle \

DEMO-LANGS := yamlscript clojure
DEMO-STAGES := glj go js
DEMO-ASSETS := docs/demo-assets
DEMO-BRANCH := demo-assets

default::

# Generate social card image from mkdocs.yaml metadata
social-card: $(DEPS)
	$(PYTHON-VENV)/bin/python3 bin/gen-social-card.py

# Generate build report from hello-world.clj
build-report: docs/build-report/index.html

docs/build-report/index.html: docs/hello-world.clj
	@mkdir -p $(dir $@)
	../bin/gloat $< -fqo /tmp/gloat-hello \
	  -Xprune,report=$@ </dev/null
	@$(RM) /tmp/gloat-hello

GLOJURE-TEST-REPORT := $(wildcard ../../glojure/test/clojure-test-suite/report.html)
ifneq (,$(GLOJURE-TEST-REPORT))
TEST-REPORT := docs/test-report/index.html
$(TEST-REPORT): $(GLOJURE-TEST-REPORT)
	mkdir -p $(dir $@)
	cp -p $< $@
endif

# Build main site (production)
site: $(DEPS) social-card build-report demo-assets $(TEST-REPORT)
	mkdocs build -d $@

# Serve locally with MkDocs
serve: $(DEPS) social-card build-report demo-assets $(TEST-REPORT)
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

# Extract pre-compiled demo assets from demo-assets branch and source
# files from demo/ for the static WASM demo page
demo-assets: $(DEPS) $(DEMO-ASSETS)/.built

$(DEMO-ASSETS)/.built:
	@echo "Extracting demo assets..."
	DEMO_BRANCH=$(DEMO-BRANCH) \
	  util/build-demo-assets $(DEMO-ASSETS) $(DEMO-PROGRAMS)
