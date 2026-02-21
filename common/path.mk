path-bb: $(BB)
	@echo "$(abspath $<)"

path-brotli: $(BROTLI)
	@echo "$(abspath $<)"

path-gh: $(GH)
	@echo "$(abspath $<)"

path-glj: $(GLJ)
	@echo "$(abspath $<)"

path-glojure: $(GLOJURE-DIR)
	@echo "$(abspath $<)"

path-go: $(GO)
	@echo "$(abspath $<)"

path-shellcheck: $(SHELLCHECK)
	@echo "$(abspath $<)"

path-wasmtime: $(WASMTIME)
	@echo "$(abspath $<)"

path-md2man: $(MD2MAN)
	@echo "$(abspath $<)"

path-ys: $(YS)
	@echo "$(abspath $<)"
