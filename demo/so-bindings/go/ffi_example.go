package main

/*
#cgo CFLAGS: -I..
#cgo LDFLAGS: -L.. -l:example.so
#include "example.h"
#include <stdlib.h>
*/
import "C"
import (
	"fmt"
	"unsafe"
)

func main() {
	for n := int64(1); n <= 10; n++ {
		fmt.Printf("%d! = %d\n", n, int64(C.factorial(C.longlong(n))))
	}

	world := C.CString("World")
	defer C.free(unsafe.Pointer(world))
	fmt.Println(C.GoString(C.greet(world)))

	ha := C.CString("ha")
	defer C.free(unsafe.Pointer(ha))
	fmt.Println(C.GoString(C.repeat_string(ha, 3)))

	msg := C.CString("hello from yamlscript")
	defer C.free(unsafe.Pointer(msg))
	C.shout_it(msg)

	m := C.maybe()
	if m != 0 {
		fmt.Println("maybe: true")
	} else {
		fmt.Println("maybe: false")
	}

	json := C.CString("[3,1,4,1,5,9,2,6]")
	defer C.free(unsafe.Pointer(json))
	fmt.Printf("sorted: %s\n", C.GoString(C.sort_json_array(json)))
}
