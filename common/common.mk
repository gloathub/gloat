GLOAT-VERSION := 0.1.56

GLOJURE-VERSION := 0.6.7
GLOJURE-REPO := https://github.com/glojurelang/glojure

# Gloat fork of let-go, carrying fixes not yet in a nooga/let-go
# release. Switch back to LET-GO-REPO := nooga/let-go when they land.
LET-GO-VERSION := 1.11.2-gloat.1
LET-GO-REPO := gloathub/let-go

YAMLSCRIPT-VERSION := 0.2.8
YS-VERSION := $(YAMLSCRIPT-VERSION)
LIBYS-VERSION := $(YAMLSCRIPT-VERSION)

# GLJ currently requires Go 1.24.0
GO-VERSION := 1.24.0

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
