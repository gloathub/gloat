package fixture

import lang "github.com/glojurelang/glojure/pkg/lang"

func aotCacheFn1(vr *lang.Var) lang.FnFunc1 {
	return nil
}

func LoadNS() {
	sym_clojure_DOT_core := lang.NewSymbolUnchecked("clojure.core")
	sym_println := lang.NewSymbolUnchecked("println")
	// var clojure.core/println
	var_clojure_DOT_core_println := lang.InternVarName(
		sym_clojure_DOT_core,
		sym_println,
	)
	aotExternalFn0 := aotCacheFn1(var_clojure_DOT_core_println)
	// keep
	{
		_ = aotExternalFn0("hello")
	}
}
