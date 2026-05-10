R := $(shell cd ../.. && pwd)
M := $R/.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

include $M/init.mk
include $R/common/common.mk
include $M/clean.mk
include $M/shell.mk

override PATH := $R/bin:$(PATH)
export LD_LIBRARY_PATH := $(shell pwd)

NAME := example
SOURCE := $(NAME).ys
LIBRARY := $(NAME).$(SO)
LIB-LIBRARY := lib$(LIBRARY)
HEADER := $(NAME).h

MAKES-CLEAN := \
  $(LIBRARY) \
  $(HEADER) \
  $(LIB-LIBRARY) \

ALL-LANGS := \
  ada \
  c \
  cpp \
  crystal \
  csharp \
  d \
  dart \
  delphi \
  fortran \
  go \
  haskell \
  java \
  julia \
  lua \
  nim \
  nodejs \
  perl \
  python \
  raku \
  ruby \
  rust \
  v \
  zig \

# Langs with toolchain issues on macOS (bugs in upstream .mk files)
# delphi: fpc 3.2.2 macOS download missing from SourceForge
# lua: LuaJIT fails to build on arm64
# perl: perl.mk site_perl arch path hardcoded to x86_64-linux
# v: vlang.mk OA mapping wrong (macos should be macos_arm64)
SKIP-MACOS := \
  delphi \
  lua \
  perl \
  v \

# Langs with known issues on Linux CI
SKIP-LINUX := \
  delphi \

ifdef IS-MACOS
LANGS ?= $(filter-out $(SKIP-MACOS),$(ALL-LANGS))
else
LANGS ?= $(filter-out $(SKIP-LINUX),$(ALL-LANGS))
endif

TESTS := $(LANGS:%=test-%)
CLEAN := $(LANGS:%=clean-%)

test: $(TESTS)

clean:: $(CLEAN)

$(TESTS):
	make --no-p -C $(@:test-%=%) test

$(CLEAN):
	make --no-p -C $(@:clean-%=%) clean
