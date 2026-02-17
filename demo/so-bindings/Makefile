R := $(shell cd ../.. && pwd)
M := $R/.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

include $M/init.mk
include $R/common/common.mk
include $M/clean.mk
include $M/shell.mk

override export PATH := $R/bin:$(PATH)
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

LANGS := \
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

TESTS := $(LANGS:%=test-%)
CLEAN := $(LANGS:%=clean-%)

test: $(TESTS)

clean:: $(CLEAN)

$(TESTS):
	make --no-p -C $(@:test-%=%) test

$(CLEAN):
	make --no-p -C $(@:clean-%=%) clean
