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

# Langs that need toolchains broken or unavailable on macOS
SKIP-MACOS := \
  csharp \
  d \
  dart \
  delphi \
  fortran \
  haskell \

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
