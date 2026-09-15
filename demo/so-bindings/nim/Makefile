BINDING := nim
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).nim


run: $(PROGRAM)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
endif
	nim c -r --hints:off \
	  --out:$(BUILD) \
	  --passL:"-L$(SO-BINDINGS-DIR)" \
	  $<
