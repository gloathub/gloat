BINDING := rust
include ../../../common/so-bindings.mk

MAKES-CLEAN := \
  Cargo.lock \
  target/ \

PROGRAM := src/main.rs


run: $(PROGRAM) | $(CARGO)
	$(dir $(CARGO))cargo run --quiet
