BINDING := ghc
include ../../../common/so-bindings.mk

GMP-SO := /usr/lib/x86_64-linux-gnu/libgmp.so.10
GMP-LINK := $(LOCAL-PREFIX)/lib/libgmp.so

PROGRAM := ffi_$(NAME).hs


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(GMP-LINK)
	$(GHC) -o $@ $< \
	  -v0 -no-keep-hi-files -no-keep-o-files \
	  -optl-L$(SO-BINDINGS-DIR) -optl-l:$(NAME).so \
	  -optl-L$(LOCAL-PREFIX)/lib -optl-lgmp

$(GMP-LINK):
	ln -sf $(GMP-SO) $@
