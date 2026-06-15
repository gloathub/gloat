GLOAT-VERSION := 0.1.47

GLOJURE-VERSION := 0.6.5-rc33
GLOJURE-REPO := https://github.com/glojurelang/glojure

# GLJ currently requires Go 1.24.0
GO-VERSION := 1.24.0

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
