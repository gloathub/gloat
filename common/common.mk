GLOJURE-VERSION := 0.6.5-rc25
GLOJURE-REPO := https://github.com/gloathub/glojure

# GLJ currently requires Go 1.24.0
GO-VERSION := 1.24.0

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
