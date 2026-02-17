BINDING := fpc
include ../../../common/so-bindings.mk

MAKES-CLEAN += \
  ffi_$(NAME).o \

PROGRAM := ffi_$(NAME).pas


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).so
	$(FPC) -o$@ \
	  -FU. \
	  -Fl$(SO-BINDINGS-DIR) \
	  $< > /dev/null
