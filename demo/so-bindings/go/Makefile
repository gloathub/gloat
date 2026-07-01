BINDING := go
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).go


run: $(PROGRAM)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
endif
	CGO_ENABLED=1 go run $<
