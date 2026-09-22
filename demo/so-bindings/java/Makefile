BINDING := java
include ../../../common/so-bindings.mk

PROGRAM := FfiExample.java


run: $(PROGRAM)
	javac -d $(LOCAL-CACHE) $<
	java -cp $(LOCAL-CACHE) \
	  --enable-native-access=ALL-UNNAMED \
	  FfiExample
