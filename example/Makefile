M := ../.cache/makes
$(shell [ -d $M ] || (git clone -q https://github.com/makeplus/makes $M))

DEFAULT-FILE := yamlscript/99-bottles-of-beer.ys

include $M/init.mk
include ../common/common.mk
include $M/babashka.mk
include $M/glojure.mk
include $M/go.mk
include $M/git.mk
include $M/jq.mk
include $M/python.mk
include $M/wasmtime.mk
include $M/yamlscript.mk
include $M/clean.mk
include $M/shell.mk

override export PATH := $(GIT-REPO-DIR)/bin:$(PATH)

ifndef FILE
FILE := $(DEFAULT-FILE)
endif
override FILE := $(FILE:example/%=%)

SRC-FILE := $(FILE)
NAME := $(notdir $(basename $(FILE)))

SRC-EXT  := $(suffix $(SRC-FILE))
GO-FILE  := $(NAME).go
CLJ-FILE := $(NAME).clj
BB-FILE  := $(NAME).bb
GLJ-FILE := $(NAME).glj
GO-DIR   := $(NAME)/
BINARY   := $(NAME)
LIBRARY  := $(NAME).$(SO)
WASM-P1  := $(NAME).wasm
WASM-JS  := $(NAME).js

TARGETS := \
  $(CLJ-FILE) \
  $(BB-FILE) \
  $(GLJ-FILE) \
  $(GO-DIR) \
  $(BINARY) \
  $(LIBRARY) \
  $(WASM-P1) \
  $(WASM-JS) \

YS-FILES := $(wildcard *.ys)
TARGET-NAMES := $(YS-FILES:%.ys=%)
WASM-EXEC-JS := wasm_exec.js
CONFIG-YAML := config.yaml
CONFIG-JSON := config.json

SERVE-DEPS := \
  $(BB) \
  $(CONFIG-JSON) \
  $(GLJ) \
  $(GLOJURE-DIR) \
  $(GO) \
  $(JQ) \
  $(PYTHON-VENV) \
  $(WASM-EXEC-JS) \
  $(YS) \
  first-wasm

BUILD-DEPS := \
  $(GIT-REPO-DIR)/ys/src/ys/v0.clj \

MAKE := $(MAKE) --no-pr


serve starting-this-demo-might-take-120-seconds-but-it-is-worth-it: $(SERVE-DEPS)
	python bin/server.py

first-wasm:
	$(MAKE) factorial.js FILE=yamlscript/factorial.ys
	$(RM) factorial.js

test:
	@$(MAKE) clean
	$(MAKE) run-bin a=3
	@$(MAKE) clean
	# $(MAKE) run-clj a=3
	@$(MAKE) clean
	$(MAKE) run-wasm a=3
	@$(MAKE) clean

dev-container-setup: $(DEV-CONTAINER-SETUP-DEPS)

clean::
	git clean -dxf .

go:   $(GO-FILE)
clj:  $(CLJ-FILE)
bb:   $(BB-FILE)
glj:  $(GLJ-FILE)
dir:  $(GO-DIR)
bin:  $(BINARY)
lib:  $(LIBRARY)
wasm: $(WASM-P1)
js:   $(WASM-JS)

run-bin: $(BINARY)
	./$<$(if $(a1), '$(a1)')$(if $(a2), '$(a2)')$(if $(a3), '$(a3)')$(if $(a), $(a))

run-wasm: $(WASM-P1) $(WASMTIME)
	wasmtime $<$(if $(a1), '$(a1)')$(if $(a2), '$(a2)')$(if $(a3), '$(a3)')$(if $(a), $(a))

run-bb: $(BB-FILE) $(BB)
	bb $<$(if $(a1), '$(a1)')$(if $(a2), '$(a2)')$(if $(a3), '$(a3)')$(if $(a), $(a))

check-a:
ifdef a
	@echo 'a= not supported for this target. Use a1=... a2=... a3=...'
	@exit 1
endif

run-js: check-a $(WASM-JS) $(CLJ-FILE) $(GLJ-FILE) $(GO-FILE) $(WASM-EXEC-JS) $(PYTHON-VENV)
	cp $(SRC-FILE) $(NAME)$(SRC-EXT)
	ys -J $(CONFIG-YAML) | \
	jq --arg prog '$(NAME)' \
	  --arg srcExt '$(SRC-EXT)' \
	  --arg a1 '$(a1)' --arg a2 '$(a2)' --arg a3 '$(a3)' \
	  '. + {program: $$prog, sourceExt: $$srcExt, args: ({a1:$$a1,a2:$$a2,a3:$$a3} | with_entries(select(.value != "")))}' | \
	jq -f bin/config.jq \
	  > $(CONFIG-JSON)
	@echo "Starting server on http://localhost:8080"
	@echo "Press Ctrl+C to stop"
	python -m http.server 8080

$(CONFIG-JSON): $(CONFIG-YAML) $(YS)
	ys -J $< > $@

$(WASM-EXEC-JS): $(GO)
	cp $(GO-LOCAL)/lib/wasm/wasm_exec.js $@

$(TARGETS): $(SRC-FILE) $(BUILD-DEPS)
	gloat $< -o $@

# Custom rule to extract Go source from directory build
$(GO-FILE): $(SRC-FILE) $(BUILD-DEPS)
	@$(RM) -rf $(NAME)
	@gloat $< -o $(GO-DIR) > /dev/null
	@cat $(GO-DIR)/pkg/*/core/loader.go > $@
	@$(RM) -rf $(GO-DIR)
