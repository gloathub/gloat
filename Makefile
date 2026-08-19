R := https://github.com/makeplus/makes
M := .cache/makes
$(shell [ -d '$M' ] || git clone -q $R '$M')

include $M/init.mk

include common/common.mk

include $M/graalvm.mk
include $M/babashka.mk
include $M/cljfmt.mk
include $M/gh.mk
include $M/git.mk
include $M/glojure.mk
include $M/go.mk
include $M/hy.mk
include $M/janet.mk
include $M/joker.mk
include $M/jolt.mk
include $M/lein.mk
include $M/let-go.mk
include $M/md2man.mk
include $M/perl.mk
include $M/phel.mk
include $M/shellcheck.mk
include $M/wasmtime.mk
include $M/yamlscript.mk
include $M/zprint.mk

ifneq ($(GLOAT_JOLT),)
JOLT := $(GLOAT_JOLT)
override PATH := $(dir $(JOLT)):$(PATH)
endif

include $M/brotli.mk
include $M/python.mk

include $M/clean.mk
include $M/shell.mk

unexport PERL5OPT PERL5LIB

GLOJURE-DIR-EXPLICIT := $(GLOJURE_DIR)
GLOJURE-DIR ?= $(or $(GLOJURE_DIR),$(LOCAL-CACHE)/glojure-$(GLOJURE-VERSION))
GLOJURE-DOWN := $(GLOJURE-REPO)/releases/download/v$(GLOJURE-VERSION)/$(GLOJURE-TAR)

ifneq ($(GLOJURE-DIR-EXPLICIT),)
export GLOJURE_DIR := $(GLOJURE-DIR)
GLJ-HOST-PLATFORM := $(or $(and $(wildcard $(GO)),$(shell $(GO) env GOOS)_$(shell $(GO) env GOARCH)),linux_amd64)
ifneq (,$(wildcard $(GLOJURE-DIR)/bin/$(GLJ-HOST-PLATFORM)/glj))
GLJ := $(GLOJURE-DIR)/bin/$(GLJ-HOST-PLATFORM)/glj
override PATH := $(dir $(GLJ)):$(PATH)
endif
endif

$(GLOJURE-DIR):
	@echo "* Cloning glojure v$(GLOJURE-VERSION) locally" >&2
	git clone -q -b v$(GLOJURE-VERSION) --config advice.detachedHead=false \
	  $(GLOJURE-REPO) $@

# A sibling checkout is the development override. Installed copies use
# a shallow checkout of the pinned release under Gloat's cache.
LET-GO-DEV-SRC := $(GIT-REPO-DIR)/../let-go
LET-GO-CACHE-SRC := $(GIT-REPO-DIR)/.cache/let-go
LET-GO-SRC ?= $(if $(wildcard $(LET-GO-DEV-SRC)),$(LET-GO-DEV-SRC),$(LET-GO-CACHE-SRC))
LG-DEV-STAMP := $(LET-GO-LOCAL)/.source-revision

$(LET-GO-SRC):
	@echo "* Cloning let-go v$(LET-GO-VERSION) locally" >&2
	git clone -q --depth 1 -b v$(LET-GO-VERSION) \
	  --config advice.detachedHead=false \
	  https://github.com/$(LET-GO-REPO) '$@'

# A sibling checkout is the development override. Installed copies use a
# shallow checkout of the pinned Go runtime module under Gloat's cache.
YS-V0-GLJ-DEV-SRC := $(ROOT)/repos/ys-v0-glj
YS-V0-GLJ-CACHE-SRC := $(ROOT)/.cache/ys-v0-glj
YS-V0-GLJ-DEV-DIR := $(if $(wildcard $(YS-V0-GLJ-DEV-SRC)),$(YS-V0-GLJ-DEV-SRC))
YS-V0-GLJ-DIR ?= $(or $(YS-V0-GLJ-DEV-DIR),$(YS-V0-GLJ-CACHE-SRC))

$(YS-V0-GLJ-DIR):
	@echo "* Cloning ys-v0-glj $(YS-V0-GLJ-VERSION) locally" >&2
	git clone -q --depth 1 -b $(YS-V0-GLJ-VERSION) \
	  --config advice.detachedHead=false \
	  https://github.com/gloathub/ys-v0-glj '$@'

include common/path.mk

MAN-PAGES := \
  man/man1/gloat.1 \
  man/man1/gloat-install.1 \
  man/man1/gloat-repl.1 \
  man/man1/gloat-tutorial.1 \
  man/man1/gloat-go-interop.1 \
  man/man1/gloat-java-interop.1 \

PATH-DEPS := \
  $(BB) \
  $(GLJ) \
  $(GLOJURE-DIR) \
  $(GO) \
  $(YS-V0-GLJ-DIR) \

# Must be included after PATH-DEPS is defined; make expands the
# 'gloat-vars: $(PATH-DEPS)' prerequisites at parse time.
include common/gloat-vars.mk

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
  .nrepl-port \
  report.* \
  Changes.tmp \
  hi \
  $(TEST-CALL) \

# Disable CGO to avoid Go linker issues
export CGO_ENABLED := 0

override PATH := $(ROOT)/bin:$(ROOT)/util:$(PATH)
export PATH

export GOPRIVATE=github.com/gloathub/*

test ?= test/*.t

tests := $(wildcard $(test))

TEST-DEPS :=
ifneq (,$(filter test/shellcheck.t,$(tests)))
TEST-DEPS += $(SHELLCHECK)
endif
ifneq (,$(filter test/format.t,$(tests)))
TEST-DEPS += $(PATH-DEPS) $(WASMTIME) $(CLJFMT) $(ZPRINT)
endif
ifneq (,$(filter %-bb.t %-bin.t,$(tests)))
TEST-DEPS += $(TEST-CALL)
endif
TEST-DEPS += $(PERL)

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

gloat-version:
	@echo '$(GLOAT-VERSION)'

gloat-git-dir:
	@echo '$(GIT-REPO-DIR)'

work-init work-pull work-save work-status work-log work-remove:
	@make-do $@ '$(CURDIR)'

man: $(MAN-PAGES)

update: $(MAN-PAGES)

bb: $(BB)
	$@

clj: $(CLJ)
	$@

glj: $(GLJ)
	$@

gloat:
	$@ --repl

hy: $(HY)
	$@

janet: $(JANET)
	$@

joker: $(JOKER)
	$@

jolt: $(JOLT)
	$@ repl

lein: $(LEIN)
	$@ repl

lg: lg-ensure
	$(LG)

# Build lg from a local let-go checkout into the versioned install
# slot. This lets Gloat developers test a sibling checkout in place of
# the pinned release (rerun after 'gloat --reset'). The touched tarball
# satisfies the module's download prerequisite.

lg-dev: $(GO)
	$Q [[ -d '$(LET-GO-SRC)' ]] || { \
	  echo "Error: no let-go checkout at '$(LET-GO-SRC)' (set LET-GO-SRC)"; \
	  exit 1; }
	@echo "Building lg from $(LET-GO-SRC)"
	$Q version=$$(git -C '$(LET-GO-SRC)' describe --tags --always --dirty | \
	    sed 's/^v//') && \
	  commit=$$(git -C '$(LET-GO-SRC)' rev-parse HEAD) && \
	  cd '$(LET-GO-SRC)' && \
	  GOPATH=$(abspath $(LOCAL-PREFIX)/go) \
	  GOMODCACHE=$(abspath $(LOCAL-PREFIX)/go-mod) \
	  '$(abspath $(GO))' build -ldflags \
	    "-s -w -X main.version=$$version -X main.commit=$$commit" \
	    -o lg .
	$Q mkdir -p $(LET-GO-LOCAL)/bin $(LOCAL-CACHE)
	$Q cp '$(LET-GO-SRC)/lg' $(LET-GO-LOCAL)/bin/lg
	$Q git -C '$(LET-GO-SRC)' rev-parse HEAD > '$(LG-DEV-STAMP)'
	$Q touch $(LOCAL-CACHE)/$(LET-GO-TAR) $(LET-GO-LOCAL)/bin/lg
	@echo $(LG)

# Keep the lower-vm encoder built from the same source checkout as its
# embedded runtime. A dirty checkout rebuilds every time; a clean one
# rebuilds only when HEAD changes.
lg-dev-ensure:
	$Q revision=$$(git -C '$(LET-GO-SRC)' rev-parse HEAD 2>/dev/null) || { \
	  echo "Error: '$(LET-GO-SRC)' is not a git checkout"; \
	  exit 1; \
	}; \
	installed=$$(cat '$(LG-DEV-STAMP)' 2>/dev/null || true); \
	dirty=$$(git -C '$(LET-GO-SRC)' status --porcelain --untracked-files=no); \
	if [[ ! -x '$(LG)' || -n "$$dirty" || "$$installed" != "$$revision" ]]; then \
	  $(MAKE) --no-print-directory lg-dev > /dev/null; \
	fi

# Make sure lg is installed: an existing binary wins, then a local
# let-go checkout (built via lg-dev; the checkout wins over download,
# like GLOJURE_DIR), then the release download. Explicit sequencing
# instead of rule prereqs so a failed download can't undo a build.
lg-ensure:
	$Q if [[ -x '$(LG)' ]]; then :; \
	elif [[ -n '$(LET-GO-SRC)' && -d '$(LET-GO-SRC)' ]]; then \
	  $(MAKE) --no-print-directory lg-dev > /dev/null; \
	else \
	  $(MAKE) --no-print-directory '$(LG)'; \
	fi

.PHONY: lg-ensure lg-dev-ensure

phel: $(PHEL)
	$(if $(shell command -v rlwrap),rlwrap )$@

which-bb: $(BB)
	@echo $<

which-lein: $(LEIN)
	@echo $<

which-clj: $(CLOJURE)
	@echo $<

which-jolt: $(JOLT)
	@echo $<

which-lg: lg-ensure
	@echo '$(abspath $(LG))'

test: $(TEST-DEPS)
	prove$(if $v, -v) $(test)

test-so-bindings:
	$(MAKE) --no-p -C demo/so-bindings test

test-docker:
ifneq (,$(wildcard .cache/local/babashka-*/bin/bb))
	@echo 'Run first: make distclean'
	@exit 1
endif
	make-do $@

GLJ-WASM := www/docs/repl/glj.wasm
GLJ-WASM-EXEC := www/docs/repl/wasm_exec.js

GLOJURE-BUILD-DIR := $(GLOJURE-DIR)

$(GLJ-WASM): $(GLJ) $(GO) $(GLOJURE-BUILD-DIR)
	@mkdir -p $(dir $@)
	cd $(GLOJURE-BUILD-DIR)/cmd/glj && \
	  GOOS=js GOARCH=wasm CGO_ENABLED=0 $(GO) build \
	    -ldflags "-X github.com/glojurelang/glojure/pkg/runtime.version=$(GLOJURE-VERSION)" \
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

SERVE-DIR ?= .

python-local-server: $(PYTHON)
	cd '$(SERVE-DIR)' && $(PYTHON) -m http.server

annoucement:
	@make-do $@ $(GLOJURE-VERSION)

release: $(GH) $(PERL)
	@$(if $(filter command line,$(origin VERSION)),,\
	  $(error VERSION is required on the command line))
	@$(if $(filter command line,$(origin GLJ-VERSION)),,\
	  $(error GLJ-VERSION is required on the command line))
	$(eval RELEASE_VER := $(patsubst v%,%,$(VERSION)))
	$(eval GLJ_VER := $(patsubst v%,%,$(GLJ-VERSION)))
	$(eval RELEASE_BRANCH := $(or $(GLOAT_RELEASE_BRANCH),$(GLOAT-RELEASE-BRANCH)))
	$(if $(RELEASE_BRANCH),GLOAT_RELEASE_BRANCH="$(RELEASE_BRANCH)" )make-do $@ $(RELEASE_VER) "$(MESSAGE)" "$(GLJ_VER)"

GLJ-PLATFORM-linux-int64 := linux_amd64
GLJ-PLATFORM-linux-arm64 := linux_arm64
GLJ-PLATFORM-macos-int64 := darwin_amd64
GLJ-PLATFORM-macos-arm64 := darwin_arm64
GLJ-PLATFORM := $(GLJ-PLATFORM-$(OS-ARCH))

repl:
	GLJ_VERSION=$(GLOJURE-VERSION) $(MAKE) --no-print -C $(GLOJURE-DIR) build
	PATH=$(GLOJURE-DIR)/bin/$(GLJ-PLATFORM):$(PATH) gloat --repl

build-glj-from-source: $(GO) $(GLOJURE-DIR)
	cd $(GLOJURE-DIR) && \
	  git checkout gloat 2>/dev/null; \
	  git pull origin gloat
	cd $(GLOJURE-DIR)/cmd/glj && \
	  GOBIN=$(LOCAL-BIN) go install .
	@echo "Built $(GLJ) from source"

man/man1/gloat.1: ReadMe.md $(MD2MAN) $(PERL)
	@mkdir -p man/man1
	$(PERL) -0777 -pe \
	    's/^\[!\[.*?\)\n\n//msg; s/\[([^\]]+)\]\([^)]+\)/$$1/g' \
	    ReadMe.md | \
	  grep -v '^<img ' | \
	  $(MD2MAN) > $@

man/man1/%.1: doc/%.md $(MD2MAN) $(PERL)
	@mkdir -p man/man1
	$(PERL) -0777 -pe \
	    's/\[([^\]]+)\]\([^)]+\)/$$1/g' \
	    $< | \
	  $(MD2MAN) > $@
