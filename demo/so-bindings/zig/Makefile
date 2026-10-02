BINDING := zig
# Zig 0.15.2 fails to link against the Xcode 26.4 SDK on macOS;
# fixed in 0.16.0 (arm64e-macos TBD entries now match aarch64-macos)
ZIG-VERSION := 0.16.0
include ../../../common/so-bindings.mk

MAKES-CLEAN += \
  $(BUILD).o \

PROGRAM := ffi_$(NAME).zig


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(HEADER)
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).$(SO)
	zig build-exe $< \
	  -lc \
	  -I$(SO-BINDINGS-DIR) \
	  -L$(SO-BINDINGS-DIR) \
	  --library example \
	  --cache-dir $(LOCAL-CACHE)/zig-cache \
	  -femit-bin=$@
