module main

#flag -I..
#flag -L..
#flag -l:example.so

#include "example.h"

fn C.factorial(n i64) i64
fn C.greet(name &u8) &u8
fn C.repeat_string(s &u8, n i64) &u8
fn C.shout_it(s &u8)
fn C.maybe() int
fn C.sort_json_array(s &u8) &u8

fn main() {
	for n in i64(1) .. 11 {
		println('${n}! = ${C.factorial(n)}')
	}

	unsafe {
		println(cstring_to_vstring(C.greet(c'World')))
		println(cstring_to_vstring(C.repeat_string(c'ha', 3)))
	}

	C.shout_it(c'hello from yamlscript')

	m := C.maybe()
	println('maybe: ${if m != 0 { "true" } else { "false" }}')

	unsafe {
		json_input := c'[3,1,4,1,5,9,2,6]'
		println('sorted: ${cstring_to_vstring(C.sort_json_array(json_input))}')
	}
}
