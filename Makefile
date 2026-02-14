GLOAT := ../bin/gloat -q
DEMO-DIR := ../demo
PROGRAMS := \
  99-bottles-of-beer \
  dragon-curve \
  fizzbuzz \
  floyds-triangle \

YS-SOURCES := $(PROGRAMS:%=$(DEMO-DIR)/yamlscript/%.ys)
CLJ-SOURCES := $(PROGRAMS:%=$(DEMO-DIR)/clojure/%.clj)

# Generate target paths for each format
YS-GLJ := $(patsubst $(DEMO-DIR)/yamlscript/%.ys,yamlscript/glj/%.glj,$(YS-SOURCES))
YS-GO := $(patsubst $(DEMO-DIR)/yamlscript/%.ys,yamlscript/go/%.go,$(YS-SOURCES))
YS-JS := $(patsubst $(DEMO-DIR)/yamlscript/%.ys,yamlscript/js/%.js,$(YS-SOURCES))

CLJ-GLJ := $(patsubst $(DEMO-DIR)/clojure/%.clj,clojure/glj/%.glj,$(CLJ-SOURCES))
CLJ-GO := $(patsubst $(DEMO-DIR)/clojure/%.clj,clojure/go/%.go,$(CLJ-SOURCES))
CLJ-JS := $(patsubst $(DEMO-DIR)/clojure/%.clj,clojure/js/%.js,$(CLJ-SOURCES))

# All targets
ALL-TARGETS := $(YS-GLJ) $(YS-GO) $(YS-JS) $(CLJ-GLJ) $(CLJ-GO) $(CLJ-JS)


default:

build: $(ALL-TARGETS)

clean:
	$(RM) -r yamlscript clojure

# Pattern rules for YAMLScript sources
yamlscript/glj/%.glj: $(DEMO-DIR)/yamlscript/%.ys
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@

yamlscript/go/%.go: $(DEMO-DIR)/yamlscript/%.ys
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@

yamlscript/js/%.js: $(DEMO-DIR)/yamlscript/%.ys
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@

# Pattern rules for Clojure sources
clojure/glj/%.glj: $(DEMO-DIR)/clojure/%.clj
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@

clojure/go/%.go: $(DEMO-DIR)/clojure/%.clj
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@

clojure/js/%.js: $(DEMO-DIR)/clojure/%.clj
	@mkdir -p $(dir $@)
	$(GLOAT) $< -o $@
