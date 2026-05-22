module GO-MODULE

go 1.24

require (
	github.com/gloathub/glojure GLOJURE-VERSION
	github.com/gloathub/gloat/ys/pkg YS-PKG-VERSION
)
EXTRA-DEPS
replace github.com/gloathub/gloat/ys/pkg => GLOAT-ROOT/ys/pkg
