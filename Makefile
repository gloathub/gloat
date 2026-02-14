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
include $M/brotli.mk

include $M/clean.mk
include $M/shell.mk

# Auto-discover YS standard library source files
YS-CLJ-FILES := $(wildcard ys/src/*/*.clj ys/src/*/*/*.clj)
YS-NAMESPACES := $(patsubst ys/src/%.clj,%,$(YS-CLJ-FILES))
YS-GLJ-FILES := $(YS-NAMESPACES:%=ys/glj/%.glj)
YS-GO-FILES  := $(YS-NAMESPACES:%=ys/pkg/%/loader.go)

# Gloat-only files with no upstream equivalents
YS-GLOAT-ONLY := \
  ys/src/ys/v0.clj \
  ys/src/ys/fs.clj \
  ys/src/ys/ipc.clj \
  ys/src/ys/json.clj \
  ys/src/ys/http.clj

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

TEST-CALL := \
  test/call \
  test/call.clj \

TEST-CALL-DEPS := \
  test/call.ys \
  $(PATH-DEPS) \

ifndef fast
TEST-CALL-DEPS += bin/gloat
endif

MAKES-CLEAN := \
  $(TEST-CALL) \

override export PATH := $(ROOT)/bin:$(ROOT)/util:$(PATH)

test ?= test/*.t


run:
	$(MAKE) --no-p -C example run-bin$(if $(FILE), FILE=$(FILE:example/%=%))$(if $a, a=$a)

path-deps: $(PATH-DEPS)

path:
	@echo "$(PATH)"

update: $(YS-GO-FILES)

save-patches:
	make-do save-patches $(YS-REPO-URL) "$(YS-GLOAT-ONLY)" $(YS-CLJ-FILES)

diff:
ifndef FILE
	@echo 'Needs FILE=...'
	exit 1
endif
	@diff -u <(curl -sl $(YS-REPO-URL)/$(FILE:ys/src/%=%)) $(FILE)

test-all: test test-example

test: $(SHELLCHECK) $(TEST-CALL)
	prove$(if $v, -v) $(test)

test-example:
	prove$(if $v, -v) test/example/*.t

test-docker:
ifneq (,$(wildcard .cache/.local/bin/bb))
	@echo 'Run first: make distclean'
	@exit 1
endif
	make-do test-docker

serve-www publish-www:
	$(MAKE) -C www $(@:%-www=%)

serve-example:
	$(MAKE) -C example serve

clean:: local-chmod
	$(MAKE) -C example $@

test/call: $(TEST-CALL-DEPS)
	gloat -qf $< -o $@

test/call.clj: $(TEST-CALL-DEPS)
	gloat -qf $< -o $@ -t bb

# v0.clj is gloat-only, don't patch from upstream
ys/src/ys/v0.clj:
	@true

# fs.clj is gloat-only (Go interop, not babashka.fs)
ys/src/ys/fs.clj:
	@true

# ipc.clj is gloat-only (Go interop, not babashka.process)
ys/src/ys/ipc.clj:
	@true

# json.clj is gloat-only (pure Clojure impl, not clojure.data.json)
ys/src/ys/json.clj:
	@true

# http.clj is gloat-only (Go net/http, not babashka.http-client)
ys/src/ys/http.clj:
	@true

force:

ys/src/%.clj: force
	@echo "Updating $@ from upstream"
	make-do update-clj $(YS-REPO-URL) $* $@

# Pattern rule: CLJ → GLJ conversion
ys/glj/%.glj: ys/src/%.clj $(GLOJURE-DIR) $(BB)
	@mkdir -p $(dir $@)
	@echo "Converting $< to $@"
	bb $(GLOJURE-DIR)/scripts/rewrite-core/rewrite.clj $< > $@

# Special rule for yamlscript/util: apply seqable? patch after compilation
ys/pkg/yamlscript/util/loader.go: ys/glj/yamlscript/util.glj $(GLJ)
	@echo "Compiling $< to $@"
	make-do compile-glj-patched $@
	@echo "Applying seqable? patch to $@"

# Pattern rule: GLJ → GO compilation
ys/pkg/%/loader.go: ys/glj/%.glj $(GLJ)
	@echo "Compiling $< to $@"
	make-do compile-glj $* $@

# std depends on fs and ipc being compiled first
ys/pkg/ys/std/loader.go: ys/glj/ys/fs.glj ys/glj/ys/ipc.glj
