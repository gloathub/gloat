GLOAT-VERSION := 0.1.62

GLOJURE-VERSION := 0.7.0
GLOJURE-REPO := https://github.com/glojurelang/glojure

LET-GO-VERSION := 1.12.2
LET-GO-REPO := nooga/let-go

YAMLSCRIPT-VERSION := 0.2.26
YS-VERSION := $(YAMLSCRIPT-VERSION)
LIBYS-VERSION := $(YAMLSCRIPT-VERSION)

# GLJ currently requires Go 1.24.0
GO-VERSION := 1.24.0

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
