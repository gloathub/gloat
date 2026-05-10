BINDING := gcc
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).cpp


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	g++ -o $@ $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
