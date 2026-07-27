package fixture

import (
	lang "github.com/glojurelang/glojure/pkg/lang"
	runtime "github.com/glojurelang/glojure/pkg/runtime"
)

var aotDirectFn0 lang.FnFunc1
var aotDirectFn1 lang.FnFunc1
var aotDirectFn2 lang.FnFunc1

func aotLinkFn1(vr *lang.Var) lang.FnFunc1 {
	return nil
}

func init() {
	runtime.RegisterNSLoader("fixture", LoadNS)
}

func LoadNS() {
	sym_clojure_DOT_core := lang.NewSymbolUnchecked("clojure.core")
	sym_println := lang.NewSymbolUnchecked("println")
	// var clojure.core/println
	var_clojure_DOT_core_println := lang.InternVarName(
		sym_clojure_DOT_core,
		sym_println,
	)
	aotExternalFn0 := aotLinkFn1(var_clojure_DOT_core_println)
	aotExternalFn1 := aotLinkFn1(var_clojure_DOT_core_println)
	// helper
	{
		aotDirectFn1 = lang.FnFunc1(func(value any) any {
			return aotExternalFn0(value)
		})
	}
	// keep
	{
		aotDirectFn0 = lang.FnFunc1(func(value any) any {
			return aotDirectFn1(value)
		})
	}
	// drop
	{
		aotDirectFn2 = lang.FnFunc1(func(value any) any {
			return aotExternalFn1(value)
		})
	}
}
