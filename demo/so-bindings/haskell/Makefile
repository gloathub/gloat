BINDING := ghc
include ../../../common/so-bindings.mk

ifdef IS-MACOS
GMP-SO := /opt/homebrew/lib/libgmp.dylib
GMP-LINK := $(LOCAL-PREFIX)/lib/libgmp.dylib
else
GMP-SO := /usr/lib/x86_64-linux-gnu/libgmp.so.10
GMP-LINK := $(LOCAL-PREFIX)/lib/libgmp.so
endif

PROGRAM := ffi_$(NAME).hs


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(GMP-LINK)
	$(GHC) -o $@ $< \
	  -v0 -no-keep-hi-files -no-keep-o-files \
	  -optl$(SO-BINDINGS-DIR)/$(NAME).$(SO) \
	  -optl-L$(LOCAL-PREFIX)/lib -optl-lgmp

$(GMP-LINK):
	ln -sf $(GMP-SO) $@
