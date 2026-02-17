BINDING := d
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).d


run: $(PROGRAM) | $(LDC)
	$(LDC) \
	  -L-L$(SO-BINDINGS-DIR) \
	  -L-l:$(NAME).so \
	  -run $<
