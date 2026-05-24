# Start a Clojure dialect repl

R := https://github.com/makeplus/makes
T := $(or $(TMPDIR),$(TEMP),$(TMP),/tmp)
ifeq (,$(wildcard $T))
$(error Can't determine temp dir)
endif
T := $T/gloathub-repl
M := $T/makes
$(shell [ -d '$M' ] || git clone -q $R '$M')

include $M/init.mk

include $M/babashka.mk
include $M/clojure.mk
include $M/gloat.mk
include $M/glojure.mk
include $M/hy.mk
include $M/janet.mk
include $M/joker.mk
include $M/lein.mk
include $M/let-go.mk
include $M/phel.mk

include $M/shell.mk

PAGER ?= less -FRX
ifeq (less,$(PAGER))
PAGER := less -FRX
endif

unexport PERL5LIB PERL5OPT


default:: help

help:
	@echo "$$HELP" | $(PAGER)

bb: $(BB)
	$@

clj: $(CLJ)
	$@

glj: $(GLJ)
	$@

gloat: $(GLOAT)
	$@ --repl

hy: $(HY)
	$@

janet: $(JANET)
	$@

joker: $(JOKER)
	$@

lein: $(LEIN)
	$@ repl

lg: $(LG)
	$@

phel: $(PHEL)
	$(if $(shell command -v rlwrap),rlwrap )$@

reset:
	$(RM) -r $T


define HELP

Instant Clojure Dialect REPLs!

Start an auto-installed Clojure dialect CLI repl client:

  make -f <(curl -sL gloathub.org/repl) <name>
  make -f <(curl -sL gloathub.org/repl) <name> <NAME>-VERSION=1.2.3

Names of repl targets and their VERSION variables:

* bb    - BABASHKA-VERSION - Babashka repl
* clj   - CLOJURE-VERSION  - Clojure repl   - Java
* glj   - GLOJURE-VERSION  - Glojure repl   - Go
* gloat - GLOAT-VERSION    - Gloat repl     - Go
* hy    - HY-VERSION       - Hy repl        - Python
* janet - JANET-VERSION    - Janet repl     - C
* joker - JOKER-VERSION    - Joker repl     - Go
* lein  - LEIN-VERSION     - Leiningen repl
* lg    - LET-GO-VERSION   - let-go repl    - Go
* phel  - PHEL-VERSION     - let-go repl    - PHP

* shell - Start a shell with all above installed
* reset - Delete the installation cache in $T
* help  - Print the help

For example:

  make -f <(curl -sL gloathub.org/repl) glj
  make -f <(curl -sL gloathub.org/repl) bb BABASHKA-VERSION=1.12.218

You can simplify this with a shell alias:

  alias repl='make -f <(curl -sL gloathub.org/repl)'
  repl glj
  repl bb BABASHKA-VERSION=1.12.218

See https://github.com/makeplus/makes for internals.
It auto-installs not only the Clojure dialects, but also the programming
languages they depend on. IOW, you don't need to have Java, Go, Python etc
installed already to try these repls.

endef
export HELP
