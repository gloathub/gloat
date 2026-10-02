R := $(shell cd ../../../.. && pwd)
M := $R/.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

include $M/init.mk
include $R/common/common.mk
include $M/go.mk
include $M/clean.mk
include $M/shell.mk

override PATH := $R/bin:$(PATH)

SOURCE-FILE := ulid.clj
BINARY := ulid

MAKES-CLEAN := \
  $(BINARY) \
  build \

unexport PERL5LIB PERL5OPT


build: $(BINARY)

run: $(SOURCE-FILE) $(GO)
	gloat --run $(SOURCE-FILE)

repl: $(GO)
	gloat --repl

$(BINARY): $(SOURCE-FILE) $(GO)
	gloat $(SOURCE-FILE) -o $(BINARY)

test: $(BINARY)
	@./$(BINARY) | grep -q '^make   : [0-9A-Z]\{26\}$$' && echo PASS || (echo FAIL; exit 1)
