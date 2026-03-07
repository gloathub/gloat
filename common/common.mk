GLOJURE-VERSION := 0.6.5-rc14
GLOJURE-REPO := https://github.com/gloathub/glojure

# Go 1.26 has a linker bug on macOS arm64 for shared library builds
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
