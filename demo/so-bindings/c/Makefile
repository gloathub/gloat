BINDING := gcc
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).c


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
ifdef IS-MACOS
	cc -o $@ \
	  $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
else
	gcc -o $@ \
	  $< \
	  -I$(SO-BINDINGS-DIR) \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
endif
