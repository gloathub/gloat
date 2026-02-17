#!/usr/bin/env python3
"""Test the greet shared library via ctypes."""

import ctypes

# Load library by name - ctypes will search LD_LIBRARY_PATH
lib = ctypes.CDLL("example.so")

# factorial(int) -> int
lib.factorial.argtypes = [ctypes.c_longlong]
lib.factorial.restype = ctypes.c_longlong

# greet(str) -> str
lib.greet.argtypes = [ctypes.c_char_p]
lib.greet.restype = ctypes.c_char_p

# repeat_string(str, int) -> str
lib.repeat_string.argtypes = [ctypes.c_char_p, ctypes.c_longlong]
lib.repeat_string.restype = ctypes.c_char_p

# shout_it(str) -> null
lib.shout_it.argtypes = [ctypes.c_char_p]
lib.shout_it.restype = None

# maybe() -> bool
lib.maybe.argtypes = []
lib.maybe.restype = ctypes.c_int

# sort_json_array(str) -> str
lib.sort_json_array.argtypes = [ctypes.c_char_p]
lib.sort_json_array.restype = ctypes.c_char_p

for n in range(1, 11):
    print(f"{n}! = {lib.factorial(n)}")

result = lib.greet(b"World")
print(result.decode())

result = lib.repeat_string(b"ha", 3)
print(result.decode())

lib.shout_it(b"hello from yamlscript")

result = lib.maybe()
print(f"maybe: {'true' if result else 'false'}")

import json
data = json.dumps([3, 1, 4, 1, 5, 9, 2, 6])
result = lib.sort_json_array(data.encode())
print(f"sorted: {result.decode()}")
