R := $(shell cd ../../.. && pwd)
M := $R/.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

include $M/init.mk
include $R/common/common.mk
include $M/$(BINDING).mk
include $M/clean.mk
include $M/shell.mk

SO-BINDINGS-DIR := $(shell cd .. && pwd)
override export PATH := $R/bin:$(PATH)
export LD_LIBRARY_PATH := $(SO-BINDINGS-DIR)

NAME := example
SOURCE := ../$(NAME).ys
LIBRARY := ../$(NAME).$(SO)
HEADER := ../$(NAME).h
BUILD := ffi_$(NAME)

MAKES-CLEAN := \
  $(BUILD) \
  $(LIBRARY) \
  $(HEADER) \
  lib$(NAME).so \

LANG-NAME := $(shell tr a-z A-Z <<<'$(BINDING)')
ifeq (GNAT,$(LANG-NAME))
SUPPORT := $(GNATMAKE)
else
SUPPORT := $($(LANG-NAME))
endif


#-------------------------------------------------------------------------------
test: init run

init: $(LIBRARY) $(HEADER) | $(SUPPORT)

$(LIBRARY) $(HEADER): $(SOURCE)
	</dev/null gloat $< -q -f -o $@ 
