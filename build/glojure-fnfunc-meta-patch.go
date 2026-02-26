package lang

import (
	"sync"
	"unsafe"
)

// FnFunc is a wrapped Go function that implements the IFn interface.
type FnFunc func(args ...any) any

var (
	_ IFn = FnFunc(nil)
)

func NewFnFunc(fn func(args ...any) any) FnFunc {
	return FnFunc(fn)
}

func (f FnFunc) Invoke(args ...any) any {
	return f(args...)
}

func (f FnFunc) ApplyTo(args ISeq) any {
	return f(seqToSlice(args)...)
}

// fnMetaStore maps function closure pointers to their metadata.
// A Go func value is internally a pointer to a function descriptor (funcval).
// We use this pointer as a unique key per closure instance.
var fnMetaStore sync.Map // uintptr -> IPersistentMap

// fnDataPtr extracts the internal pointer of a Go func value.
// In Go's runtime, a func value is a pointer to a struct whose first
// word is the code pointer and subsequent words are captured variables.
// This gives us a unique address per closure instance.
func fnDataPtr(f FnFunc) uintptr {
	return *(*uintptr)(unsafe.Pointer(&f))
}

func (f FnFunc) Meta() IPersistentMap {
	if f == nil {
		return nil
	}
	key := fnDataPtr(f)
	if key == 0 {
		return nil
	}
	if v, ok := fnMetaStore.Load(key); ok {
		return v.(IPersistentMap)
	}
	return nil
}

func (f FnFunc) WithMeta(meta IPersistentMap) any {
	if f == nil {
		return f
	}
	key := fnDataPtr(f)
	if key != 0 {
		fnMetaStore.Store(key, meta)
	}
	// Return f unchanged so that .(lang.FnFunc) type assertions still work
	return f
}
