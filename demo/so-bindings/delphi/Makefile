BINDING := fpc
include ../../../common/so-bindings.mk

MAKES-CLEAN += \
  ffi_$(NAME).o \

PROGRAM := ffi_$(NAME).pas


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).dylib
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/$(NAME).so
else
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).so
endif
	$(FPC) -o$@ \
	  -FU. \
	  -Fl$(SO-BINDINGS-DIR) \
	  $< > /dev/null
