{.passL: "-l:example.so".}

proc factorial(n: clonglong): clonglong {.importc, cdecl.}
proc greet(name: cstring): cstring {.importc, cdecl.}
proc repeat_string(s: cstring; n: clonglong): cstring {.importc, cdecl.}
proc shout_it(s: cstring) {.importc, cdecl.}
proc maybe(): cint {.importc, cdecl.}
proc sort_json_array(s: cstring): cstring {.importc, cdecl.}

for n in 1..10:
  echo $n & "! = " & $factorial(n.clonglong)

echo $greet("World")
echo $repeat_string("ha", 3)

shout_it("hello from yamlscript")

let m = maybe()
echo "maybe: " & (if m != 0: "true" else: "false")

echo "sorted: " & $sort_json_array("[3,1,4,1,5,9,2,6]")
