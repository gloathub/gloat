BINDING := fortran
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).f90


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	$(GFORTRAN) -o $@ $< \
	  $(SO-BINDINGS-DIR)/$(NAME).$(SO)
