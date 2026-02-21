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

# Override venv creation: use uv venv by Python name so uv uses its own
# installation metadata rather than probing the binary (which hangs with
# uv-managed standalone Python builds due to ensurepip).
$(PYTHON-VENV): $(PYTHON)
	@echo '+++ Installing a Python virtualenv in $@'
	uv venv --python $(PYTHON-NAME) $@
	$(if $(wildcard requirements.txt),uv pip install -r requirements.txt,true)
	@echo

REPO ?= git@github.com:gloathub/gloat

MAKES-CLEAN := site docs/demo-assets
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

# Build main site (production)
site: $(DEPS) demo-assets
	mkdocs build -d $@

# Serve locally with MkDocs
serve: $(DEPS) demo-assets
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
	@$(RM) -r $(DEMO-ASSETS)
	@mkdir -p $(DEMO-ASSETS)
	@# Extract wasm_exec.js
	git show $(DEMO-BRANCH):wasm_exec.js > $(DEMO-ASSETS)/wasm_exec.js
	@# Extract compiled files for each language/program/stage
	@for lang in $(DEMO-LANGS); do \
	  for stage in $(DEMO-STAGES); do \
	    mkdir -p $(DEMO-ASSETS)/$$lang/$$stage; \
	    for prog in $(DEMO-PROGRAMS); do \
	      git show $(DEMO-BRANCH):$$lang/$$stage/$$prog.$$stage \
	        > $(DEMO-ASSETS)/$$lang/$$stage/$$prog.$$stage; \
	    done; \
	  done; \
	done
	@# Copy source files from demo/
	@for prog in $(DEMO-PROGRAMS); do \
	  mkdir -p $(DEMO-ASSETS)/yamlscript/src; \
	  cp ../demo/yamlscript/$$prog.ys $(DEMO-ASSETS)/yamlscript/src/; \
	  mkdir -p $(DEMO-ASSETS)/clojure/src; \
	  cp ../demo/clojure/$$prog.clj $(DEMO-ASSETS)/clojure/src/; \
	done
	@# Generate config.json filtered to demo programs
	$(PYTHON-VENV)/bin/python3 -c " \
	import yaml, json, sys; \
	config = yaml.safe_load(open('../demo/config.yaml')); \
	programs = '$(DEMO-PROGRAMS)'.split(); \
	filtered = {k: v for k, v in config.items() if k in programs}; \
	json.dump(filtered, sys.stdout, indent=2)" \
	  > $(DEMO-ASSETS)/config.json
	@touch $@
	@echo "Demo assets ready in $(DEMO-ASSETS)/"
