DOTNET-VERSION := 8.0.415
BINDING := dotnet
include ../../../common/so-bindings.mk

MAKES-CLEAN := \
  bin/ \
  obj/ \

PROGRAM := Program.cs


run: $(PROGRAM)
	dotnet run $<
