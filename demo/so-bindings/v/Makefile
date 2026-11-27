BINDING := vlang
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).v


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(HEADER)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
endif
	v -o $@ $<
