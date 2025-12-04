M := .cache/makes
$(shell [ -d $M ] || (git clone -q https://github.com/makeplus/makes $M))

include $M/init.mk
include common/common.mk
include $M/babashka.mk
include $M/git.mk
include $M/glojure.mk
include $M/go.mk
include $M/shellcheck.mk
include $M/yamlscript.mk
include $M/wasmtime.mk

include $M/clean.mk
include $M/shell.mk

# Auto-discover YS standard library source files
YS-CLJ-FILES := $(wildcard ys/src/*/*.clj ys/src/*/*/*.clj)
YS-NAMESPACES := $(patsubst ys/src/%.clj,%,$(YS-CLJ-FILES))
YS-GLJ-FILES := $(YS-NAMESPACES:%=ys/glj/%.glj)
YS-GO-FILES  := $(YS-NAMESPACES:%=ys/pkg/%/loader.go)

YS-REPO-URL := \
  https://raw.githubusercontent.com/yaml/yamlscript/v0/core/src

# Mark GLJ files as precious (don't auto-delete intermediate files)
.PRECIOUS: $(YS-CLJ-FILES) $(YS-GLJ-FILES)

PATH-DEPS := \
  $(BB) \
  $(GLJ) \
  $(GLOJURE-DIR) \
  $(GO) \
  $(YS) \

override export PATH := $(GIT-REPO-DIR)/bin:$(PATH)

test ?= test/*.t


run:
	$(MAKE) --no-p -C example run-bin$(if $(FILE), FILE=$(FILE:example/%=%))$(if $a, a=$a)

path-deps: $(PATH-DEPS)

path:
	@echo "$(PATH)"

update: $(YS-GO-FILES)

diff:
ifndef FILE
	@echo 'set FILE=...'
	exit 1
endif
	@diff -u <(curl -sl $(YS-REPO-URL)/$(FILE:ys/src/%=%)) $(FILE)

test-all: test test-example

test: $(SHELLCHECK)
	prove$(if $v, -v) $(test)

test-example:
	prove$(if $v, -v) test/example/*.t

serve demo-server:
	$(MAKE) -C example serve

clean:: local-chmod
	$(MAKE) -C example $@

force:

# v0.clj is gloat-only, don't patch from upstream
ys/src/ys/v0.clj:
	@true

ys/src/%.clj: force
	@echo "Patching $@ from repo source"
	util/patch-ys-repo-source $@ $(YS-REPO-URL) > $@

# Pattern rule: CLJ → GLJ conversion
ys/glj/%.glj: ys/src/%.clj $(GLOJURE-DIR) $(BB)
	@mkdir -p $(dir $@)
	@echo "Converting $< to $@"
	bb $(GLOJURE-DIR)/scripts/rewrite-core/rewrite.clj $< > $@

# Special rule for yamlscript/util: apply seqable? patch after compilation
ys/pkg/yamlscript/util/loader.go: ys/glj/yamlscript/util.glj $(GLJ)
	@mkdir -p $(dir $@)
	@echo "Compiling $< to $@"
	@tmpdir=$$(mktemp -d) && \
	  cp -r ys/glj/* "$$tmpdir/" && \
	  cd "$$tmpdir" && \
	  ns=$$(echo "yamlscript/util" | tr '/' '.') && \
	  echo "(compile (quote $$ns))" | glj >/dev/null 2>&1 || true && \
	  cd - >/dev/null && \
	  cp "$$tmpdir/yamlscript/util/loader.go" $@ && \
	  rm -rf "$$tmpdir"
	@echo "Applying seqable? patch to $@"
	@patch -p1 < ys/patches/yamlscript-util-seqable.patch

# Pattern rule: GLJ → GO compilation
ys/pkg/%/loader.go: ys/glj/%.glj $(GLJ)
	@mkdir -p $(dir $@)
	@echo "Compiling $< to $@"
	@tmpdir=$$(mktemp -d) && \
	  cp -r ys/glj/* "$$tmpdir/" && \
	  cd "$$tmpdir" && \
	  ns=$$(echo "$*" | tr '/' '.') && \
	  echo "(compile (quote $$ns))" | glj >/dev/null 2>&1 || true && \
	  cd - >/dev/null && \
	  cp "$$tmpdir/$*/loader.go" $@ && \
	  rm -rf "$$tmpdir"
