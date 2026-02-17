BINDING := nim
include ../../../common/so-bindings.mk

PROGRAM := ffi_$(NAME).nim


run: $(PROGRAM)
	nim c -r --hints:off \
	  --out:$(BUILD) \
	  --passL:"-L$(SO-BINDINGS-DIR)" \
	  $<
