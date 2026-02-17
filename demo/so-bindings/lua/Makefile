BINDING := luajit
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).lua


run: $(PROGRAM)
	luajit $<
