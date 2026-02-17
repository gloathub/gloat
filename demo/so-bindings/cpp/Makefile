BINDING := gcc
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).cpp


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	g++ -o $@ $< \
	  -I$(SO-BINDINGS-DIR) \
	  -L$(SO-BINDINGS-DIR) \
	  -l:$(NAME).so
