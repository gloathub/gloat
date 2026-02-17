BINDING := fortran
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).f90


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM)
	$(GFORTRAN) -o $@ $< \
	  -L$(SO-BINDINGS-DIR) -l:$(NAME).so
