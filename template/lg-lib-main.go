package main

import "C"

import (
	_ "embed"
	"fmt"

	"github.com/nooga/let-go/pkg/rt"
	"github.com/nooga/let-go/pkg/vm"
LOWERED-IMPORTS
)

//go:embed program.lgb
var lgbData []byte

const programNamespace = "NAMESPACE"

func init() {
	// Runtime-only boot: core loads from its embedded bundle, so neither
	// the compiler nor the resolver is linked into the shared library.
	if _, err := rt.BootCore(); err != nil {
		panic(vm.FormatError(err))
	}
	unit, err := rt.DecodeExecUnit(lgbData)
	if err != nil {
		panic(vm.FormatError(err))
	}
	// Replays every ns chunk and drains each one's Go-native overrides.
	if err := rt.LoadProgramNamespaces(unit); err != nil {
		panic(vm.FormatError(err))
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
