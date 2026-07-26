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
	aotExternalFn1 := aotCacheFn1(var_clojure_DOT_core_println)
	aotExternalFn2 := aotCacheFn1(var_clojure_DOT_core_println)
	var closed0 any
	var closed1 any
	{
		closed0 = aotExternalFn2("keep")
	}
	{
		closed1 = "drop"
	}
	// keep
	{
		_ = closed0
		_ = aotExternalFn0("hello")
	}
}
