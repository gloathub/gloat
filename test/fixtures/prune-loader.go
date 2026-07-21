package fixture

import lang "github.com/glojurelang/glojure/pkg/lang"

func LoadNS() {
	sym_keep := lang.NewSymbol("keep")
	sym_drop := lang.NewSymbolUnchecked("drop")
	// keep
	{
		_ = sym_keep
	}
	// drop
	{
		_ = sym_drop
	}
}
