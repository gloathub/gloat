lib = "example.so"

for n in 1:10
    result = ccall((:factorial, lib), Clonglong, (Clonglong,), n)
    println("$(n)! = $(result)")
end

r = ccall((:greet, lib), Cstring, (Cstring,), "World")
println(unsafe_string(r))

r = ccall((:repeat_string, lib), Cstring, (Cstring, Clonglong), "ha", 3)
println(unsafe_string(r))

ccall((:shout_it, lib), Cvoid, (Cstring,), "hello from yamlscript")

m = ccall((:maybe, lib), Cint, ())
println("maybe: $(m != 0)")

r = ccall((:sort_json_array, lib), Cstring, (Cstring,), "[3,1,4,1,5,9,2,6]")
println("sorted: $(unsafe_string(r))")
