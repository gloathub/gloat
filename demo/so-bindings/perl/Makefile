BINDING := perl
include ../../../common/so-bindings.mk

PERL-VERSION-SHORT := $(PERL-VERSION:%.0=%)

export PERL_BADLANG := 0

PERL-FFI-PLATYPUS := \
  $(LOCAL-ROOT)/perl-$(PERL-VERSION)/lib/site_perl/$(PERL-VERSION-SHORT)/x86_64-linux/FFI/Platypus.pm

PROGRAM := ffi_$(NAME).pl


run: $(PROGRAM) $(PERL-FFI-PLATYPUS)
	perl $<

$(PERL-FFI-PLATYPUS):
	cpanm -q --notest FFI::Platypus
	touch $@
