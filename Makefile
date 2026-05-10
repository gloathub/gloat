R := https://github.com/makeplus/makes
M := .cache/makes
$(shell [ -d '$M' ] || git clone -q $R '$M')

include $M/init.mk

include common/common.mk

include $M/babashka.mk
include $M/gh.mk
include $M/git.mk
include $M/glojure.mk
include $M/go.mk
include $M/lein.mk
include $M/md2man.mk
include $M/shellcheck.mk
include $M/let-go.mk
include $M/wasmtime.mk
include $M/yamlscript.mk

include $M/brotli.mk
include $M/python.mk

include $M/clean.mk
include $M/shell.mk
include common/path.mk
include common/gloat-vars.mk

# Auto-discover YS standard library source files
YS-CLJ-FILES := $(wildcard ys/src/*/*.clj ys/src/*/*/*.clj)
YS-NAMESPACES := $(patsubst ys/src/%.clj,%,$(YS-CLJ-FILES))
YS-GLJ-FILES := $(YS-NAMESPACES:%=ys/glj/%.glj)
YS-GO-FILES  := $(YS-NAMESPACES:%=ys/go/%/loader.go)

# Gloat-only files with no upstream equivalents
YS-GLOAT-ONLY := \
  ys/src/ys/v0.clj \
  ys/src/ys/fs.clj \
  ys/src/ys/ipc.clj \
  ys/src/ys/json.clj \
  ys/src/ys/http.clj

YS-REPO-URL := \
  https://raw.githubusercontent.com/yaml/yamlscript/v0/core/src

YS-PKG-VERSION ?= v0.1.37

# Mark GLJ files as precious (don't auto-delete intermediate files)
.PRECIOUS: $(YS-CLJ-FILES) $(YS-GLJ-FILES)

MAN-PAGES := \
  man/man1/gloat.1 \
  man/man1/gloat-repl.1 \

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
TEST-CALL-DEPS += \
  bin/gloat \
  src/gloat.clj \

endif

MAKES-CLEAN := \
  report.* \
  Changes.tmp \
  $(TEST-CALL) \

# Disable CGO to avoid Go linker issues
export CGO_ENABLED := 0

override PATH := $(ROOT)/bin:$(ROOT)/util:$(PATH)
export PATH

export GOPRIVATE=github.com/gloathub/*
unexport PERL5OPT

test ?= test/*.t

tests := $(wildcard $(test))

TEST-DEPS :=
ifneq (,$(filter test/shellcheck.t,$(tests)))
TEST-DEPS += $(SHELLCHECK)
endif
ifneq (,$(filter %-bb.t %-bin.t,$(tests)))
TEST-DEPS += $(TEST-CALL)
endif

ifdef slow
  export RUN_SLOW_TESTS := true
endif


run:
	$(MAKE) --no-p -C demo run-bin$(if $(FILE), FILE=$(FILE:demo/%=%))$(if $a, a=$a)

path-deps: $(PATH-DEPS)

shell-deps: $(SHELL-DEPS)

SHELL-DEPS-MIN := \
  $(WASMTIME) \

shell-deps-min: $(SHELL-DEPS-MIN)

path:
	@echo "$(PATH)"

env:
	@echo 'export PATH="$(PATH)"'

man: $(MAN-PAGES)

update: $(YS-GO-FILES) $(MAN-PAGES)

bb: $(BB)
	$@

lein: $(LEIN)
	$@ repl

which-bb: $(BB)
	@echo $<

which-lein: $(LEIN)
	@echo $<

which-clj: $(CLOJURE)
	@echo $<

which-lg: $(LG)
	@echo $<

ys-pkg: $(YS-GO-FILES) $(GO)
	@echo "Syncing ys/go/ to ys/pkg/"
	@mkdir -p ys/pkg
	rsync -a --delete --exclude='all/' --exclude='go.mod' --exclude='go.sum' ys/go/ ys/pkg/
	@echo "Running go mod tidy in ys/pkg/"
	cd ys/pkg && go mod tidy

tag-ys-pkg:
	$(eval YS-PKG-VER := $(patsubst v%,%,$(YS-PKG-VERSION)))
	@echo "Tagging ys/pkg/v$(YS-PKG-VER)"
	git tag -a ys/pkg/v$(YS-PKG-VER) -m "Release ys/pkg v$(YS-PKG-VER)"

save-patch:
	make-do $@ $(YS-REPO-URL) "$(YS-GLOAT-ONLY)" $(YS-CLJ-FILES)

diff:
ifndef FILE
	@echo 'Needs FILE=...'
	exit 1
endif
	@diff -u <(curl -sl $(YS-REPO-URL)/$(FILE:ys/src/%=%)) $(FILE)

test: $(TEST-DEPS)
	prove$(if $v, -v) $(test)

test-so-bindings:
	$(MAKE) --no-p -C demo/so-bindings test

test-docker:
ifneq (,$(wildcard .cache/local/bin/bb))
	@echo 'Run first: make distclean'
	@exit 1
endif
	make-do $@

GLJ-WASM := www/docs/repl/glj.wasm
GLJ-WASM-EXEC := www/docs/repl/wasm_exec.js

GLOJURE-BUILD-DIR := $(GLOJURE-DIR)

$(GLJ-WASM): $(GLJ) $(GLOJURE-BUILD-DIR)
	@mkdir -p $(dir $@)
	cd $(GLOJURE-BUILD-DIR)/cmd/glj && \
	  GOOS=js GOARCH=wasm CGO_ENABLED=0 $(GO) build \
	    -ldflags "-X github.com/gloathub/glojure/pkg/runtime.version=$(GLOJURE-VERSION)" \
	    -o $(ROOT)/$@ .

$(GLJ-WASM-EXEC): $(GO)
	@mkdir -p $(dir $@)
	cp $(GOROOT)/lib/wasm/wasm_exec.js $@

glj-wasm: $(GLJ-WASM) $(GLJ-WASM-EXEC)

serve-www publish-www: glj-wasm
	$(MAKE) -C www $(@:%-www=%)

serve-demo:
	$(MAKE) -C demo serve

clean:: local-chmod
	$(MAKE) -C demo $@
	$(MAKE) -C www $@

realclean:: local-chmod
	$(MAKE) -C demo $@
	$(MAKE) -C www $@

test/call: $(TEST-CALL-DEPS)
	</dev/null gloat -qf $< -o $@

test/call.clj: $(TEST-CALL-DEPS)
	</dev/null gloat -qf $< -o $@ -t bb

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

SERVE-DIR ?= .

python-local-server: $(PYTHON)
	cd '$(SERVE-DIR)' && $(PYTHON) -m http.server

annoucement:
	@make-do $@ $(GLOJURE-VERSION)

release: $(GH)
	@$(if $(filter command line,$(origin VERSION)),,\
	  $(error VERSION is required on the command line))
	@$(if $(filter command line,$(origin GLJ-VERSION)),,\
	  $(error GLJ-VERSION is required on the command line))
	$(eval RELEASE_VER := $(patsubst v%,%,$(VERSION)))
	$(eval GLJ_VER := $(patsubst v%,%,$(GLJ-VERSION)))
	make-do $@ $(RELEASE_VER) "$(MESSAGE)" "$(GLJ_VER)"

GLJ-PLATFORM-linux-int64 := linux_amd64
GLJ-PLATFORM-linux-arm64 := linux_arm64
GLJ-PLATFORM-macos-int64 := darwin_amd64
GLJ-PLATFORM-macos-arm64 := darwin_arm64
GLJ-PLATFORM := $(GLJ-PLATFORM-$(OS-ARCH))

GO-READLINE-DIR := $(wildcard $(GLOJURE-DIR)/../go-readline)

repl:
	$(if $(GO-READLINE-DIR),\
	  GO_REPLACE="github.com/reeflective/readline=$(GO-READLINE-DIR)") \
	  GLJ_VERSION=$(GLOJURE-VERSION) $(MAKE) --no-print -C $(GLOJURE-DIR) build
	PATH=$(GLOJURE-DIR)/bin/$(GLJ-PLATFORM):$(PATH) gloat --repl

build-glj-from-source: $(GO) $(GLOJURE-DIR)
	cd $(GLOJURE-DIR) && \
	  git checkout gloat 2>/dev/null; \
	  git pull origin gloat
	cd $(GLOJURE-DIR)/cmd/glj && \
	  GOBIN=$(LOCAL-BIN) go install .
	@echo "Built $(GLJ) from source"

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
ys/go/yamlscript/util/loader.go: ys/glj/yamlscript/util.glj $(GLJ)
	@echo "Compiling $< to $@"
	make-do compile-glj-patched $@
	@echo "Applying seqable? patch to $@"

# Pattern rule: GLJ → GO compilation
ys/go/%/loader.go: ys/glj/%.glj $(GLJ)
	@echo "Compiling $< to $@"
	make-do compile-glj $* $@

man/man1/gloat.1: ReadMe.md $(MD2MAN)
	@mkdir -p man/man1
	perl -0777 -pe \
	    's/^\[!\[.*?\)\n\n//msg; s/\[([^\]]+)\]\([^)]+\)/$$1/g' \
	    ReadMe.md | \
	  grep -v '^<img ' | \
	  $(MD2MAN) > $@

man/man1/gloat-repl.1: doc/gloat-repl.md $(MD2MAN)
	@mkdir -p man/man1
	perl -0777 -pe \
	    's/\[([^\]]+)\]\([^)]+\)/$$1/g' \
	    doc/gloat-repl.md | \
	  $(MD2MAN) > $@

# std depends on fs and ipc being compiled first
ys/go/ys/std/loader.go: ys/glj/ys/fs.glj ys/glj/ys/ipc.glj
