BINDING := go
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).go


run: $(PROGRAM)
	CGO_ENABLED=1 go run $<
