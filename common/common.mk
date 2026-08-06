GLOAT-VERSION := 0.1.74

GLOJURE-VERSION := 0.7.10
GLOJURE-REPO := https://github.com/glojurelang/glojure

LET-GO-VERSION := 1.12.2
LET-GO-REPO := nooga/let-go

YAMLSCRIPT-VERSION := 0.2.27
YS-VERSION := $(YAMLSCRIPT-VERSION)
LIBYS-VERSION := $(YAMLSCRIPT-VERSION)

GRAAL-CLOJURE-VERSION := 1.12.0

JOLT-VERSION := 0.5.14

# Jolt v0.5.14 publishes an Intel macOS binary, but the current Makes
# platform table predates that asset.
OA-macos-int64 := x86_64-macos

# GLJ currently requires Go 1.24.0
GO-VERSION := 1.24.0

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
