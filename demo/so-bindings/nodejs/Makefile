BINDING := node
include ../../../common/so-bindings.mk

MAKES-CLEAN := \
  node_modules/ \

PROGRAM := ffi_$(NAME).js


run: $(PROGRAM) node_modules
	node $<

node_modules: package.json
	npm install --silent
