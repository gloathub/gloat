BINDING := vlang
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).v


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(HEADER)
	v -o $@ $<
