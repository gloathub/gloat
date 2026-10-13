BINDING := gnat
include ../../../common/so-bindings.mk

MAKES-CLEAN += \
  ffi_$(NAME).ali \
  ffi_$(NAME).o \

PROGRAM := ffi_$(NAME).adb


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	gnatmake $< -o $@ \
	  -D. \
	  -largs \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
