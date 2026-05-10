BINDING := gcc
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).c


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	gcc -o $@ \
	  $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
