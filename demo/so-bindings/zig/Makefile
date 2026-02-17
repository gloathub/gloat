BINDING := zig
include ../../../common/so-bindings.mk

MAKES-CLEAN += \
  $(BUILD).o \

PROGRAM := ffi_$(NAME).zig


run: $(BUILD)
	./$<

$(BUILD): $(PROGRAM) $(HEADER)
	ln -sf $(NAME).$(SO) $(SO-BINDINGS-DIR)/lib$(NAME).so
	zig build-exe $< \
	  -lc \
	  -I$(SO-BINDINGS-DIR) \
	  -L$(SO-BINDINGS-DIR) \
	  --library example \
	  --cache-dir $(LOCAL-CACHE)/zig-cache \
	  -femit-bin=$@
