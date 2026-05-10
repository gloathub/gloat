BINDING := vlang
include ../../../common/so-bindings.mk

# Fix: vlang.mk has OA-macos-arm64 := macos but file is macos_arm64
ifdef IS-MACOS
ifdef IS-ARM
VLANG-ZIP := v_macos_arm64.zip
else
VLANG-ZIP := v_macos_x86_64.zip
endif
VLANG-DOWN := https://github.com/vlang/v/releases/download/$(VLANG-VERSION)/$(VLANG-ZIP)
endif

PROGRAM := ffi_$(NAME).v


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(HEADER)
ifdef IS-MACOS
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
endif
	v -o $@ $<
