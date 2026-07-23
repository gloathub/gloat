package main

import "C"

import (
	"bytes"
	_ "embed"
	"fmt"

	"github.com/nooga/let-go/pkg/bytecode"
	"github.com/nooga/let-go/pkg/compiler"
	"github.com/nooga/let-go/pkg/resolver"
	"github.com/nooga/let-go/pkg/rt"
	"github.com/nooga/let-go/pkg/vm"
LOWERED-IMPORTS
)

//go:embed program.lgb
var lgbData []byte

const programNamespace = "NAMESPACE"

func init() {
	ctx := compiler.NewCompiler(vm.NewConsts(), rt.NS("user"))
	rt.SetNSLoader(resolver.NewNSResolver(ctx, nil))

	resolve := func(nsName, name string) *vm.Var {
		n := rt.DefNSBare(nsName)
		if v := n.LookupLocal(vm.Symbol(name)); v != nil {
			return v
		}
		return n.DefStub(name)
	}
	unit, err := bytecode.DecodeToExecUnit(bytes.NewReader(lgbData), resolve)
	if err != nil {
		panic(vm.FormatError(err))
	}
	for _, name := range unit.NSOrder {
		chunk := unit.NSChunks[name]
		if chunk == nil || chunk == unit.MainChunk {
			continue
		}
		f := vm.NewFrame(chunk, nil)
		_, err := f.RunProtected()
		vm.ReleaseFrame(f)
		if err != nil {
			panic(vm.FormatError(fmt.Errorf("loading namespace %s: %w", name, err)))
		}
		rt.ApplyGoOverrides(rt.LookupNS(name))
	}
}

func invoke(name string, args ...vm.Value) vm.Value {
	v := rt.NS(programNamespace).LookupLocal(vm.Symbol(name))
	if v == nil {
		panic(fmt.Sprintf("exported var %s/%s not found", programNamespace, name))
	}
	result, err := vm.RootExecContext.Invoke(v, args)
	if err != nil {
		panic(vm.FormatError(err))
	}
	return result
}

EXPORT-FUNCTIONS

func main() {
	// Required for c-shared build mode.
}
