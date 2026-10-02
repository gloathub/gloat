BINDING := gcc
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).cpp


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
ifdef IS-MACOS
	/usr/bin/c++ -o $@ $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
else
	g++ -o $@ $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
endif
