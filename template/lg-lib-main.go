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
	// Same skip+drain loop as lg-main.go. rt.LoadProgramNamespaces would
	// replay MainChunk (wrong if the driver calls -main) and is not in the
	// pinned let-go v1.12.2 release; keep the explicit loop so the pin and
	// the semantics stay aligned.
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
