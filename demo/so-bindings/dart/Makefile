BINDING := dart
include ../../../common/so-bindings.mk

MAKES-CLEAN := \
  .dart_tool/ \
  pubspec.lock \

DART-DEPS := .dart_tool/package_config.json

PROGRAM := ffi_$(NAME).dart


run: $(PROGRAM) $(DART-DEPS)
	dart run $<

$(DART-DEPS): pubspec.yaml
	dart pub get
	touch $@
