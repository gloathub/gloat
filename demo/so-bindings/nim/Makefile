BINDING := nim
include ../../../common/so-bindings.mk

# Fix: nim.mk has OA-macos-arm64 := macos_arm64 but file is macosx_arm64
ifdef IS-MACOS
ifdef IS-ARM
NIM-TAR := nim-$(NIM-VERSION)-macosx_arm64.tar.xz
else
NIM-TAR := nim-$(NIM-VERSION)-macosx_x64.tar.xz
endif
NIM-DOWN := https://nim-lang.org/download/$(NIM-TAR)
endif

PROGRAM := ffi_$(NAME).nim


run: $(PROGRAM)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
endif
	nim c -r --hints:off \
	  --out:$(BUILD) \
	  --passL:"-L$(SO-BINDINGS-DIR)" \
	  $<
