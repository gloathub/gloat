R := $(shell cd ../../.. && pwd)
M := $R/.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

include $M/init.mk
include $R/common/common.mk
include $M/go.mk
include $M/clean.mk
include $M/shell.mk

override PATH := $R/bin:$(PATH)

DEMO-NAME := picker
GO-MODULE := gloat-demo/$(DEMO-NAME)
BUILD-DIR := build

BINARY := $(DEMO-NAME)
SOURCE-FILE ?= $(DEMO-NAME).clj
SHIM-FILE := shim.go

MAKES-CLEAN := \
  $(BINARY) \
  $(BUILD-DIR) \


build: $(BINARY)

run: $(BINARY)
	./$(BINARY)

$(BINARY): $(SOURCE-FILE) $(SHIM-FILE) $(GO)
	</dev/null gloat -q -f --module=$(GO-MODULE) $< -o $(BUILD-DIR)/
	perl -pe 's|GO-MODULE|$(GO-MODULE)|g' $(SHIM-FILE) > $(BUILD-DIR)/main.go
	echo 'require github.com/charmbracelet/bubbletea v1.3.10' >> $(BUILD-DIR)/go.mod
	cd $(BUILD-DIR) && $(GO) mod tidy && $(GO) build -o ../$(BINARY) .
