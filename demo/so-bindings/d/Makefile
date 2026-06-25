BINDING := d
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).d


run: $(PROGRAM) | $(LDC)
	$(LDC) \
	  -L$(SO-BINDINGS-DIR)/$(NAME).$(SO) \
	  -run $<
