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

override export PATH := $(GIT-REPO-DIR)/bin:$(PATH)

test ?= test/*.t


run:
	$(MAKE) --no-p -C example run-bin$(if $(FILE), FILE=$(FILE:example/%=%))$(if $a, a=$a)

path-deps: $(PATH-DEPS)

path:
	@echo "$(PATH)"

update: $(YS-GO-FILES)

save-patches:
	@for f in $(filter-out $(YS-GLOAT-ONLY),$(YS-CLJ-FILES)); do \
	  clj=$${f#ys/src/}; \
	  name=$$(echo "$$clj" | tr '/' '-' | sed 's/\.clj$$//'); \
	  diff -u <(curl -sL $(YS-REPO-URL)/$$clj) $$f 2>/dev/null \
	    | sed '1,2s/\t.*//' > ys/patch/$$name.patch || true; \
	  if [ ! -s ys/patch/$$name.patch ]; then \
	    rm -f ys/patch/$$name.patch; \
	  else \
	    echo "Saved ys/patch/$$name.patch"; \
	  fi; \
	done
	@echo "Patches saved to ys/patch/"

diff:
ifndef FILE
	@echo 'set FILE=...'
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
	docker run --rm -it \
	  -w /work \
	  -v $$PWD:/work \
	  ubuntu:24.04 \
	  bash -c ' \
	    set -x && \
	    apt update && \
	    apt install -y curl git make xz-utils && \
	    git config --global --add safe.directory /work && \
	    export PERL_BADLANG=0 && \
	    make test && \
	    chown -R '"$$(id -u):$$(id -g) .cache"

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
	@curl -sL $(YS-REPO-URL)/$*.clj -o $@
	@patch_file=ys/patch/$$(echo "$*" | tr '/' '-').patch; \
	  if [ -f "$$patch_file" ]; then \
	    echo "Applying $$patch_file"; \
	    patch --no-backup-if-mismatch -p0 < "$$patch_file"; \
	  fi

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
	@patch --no-backup-if-mismatch -p1 < ys/patch/yamlscript-util-seqable.patch

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

# std depends on fs and ipc being compiled first
ys/pkg/ys/std/loader.go: ys/glj/ys/fs.glj ys/glj/ys/ipc.glj
