local ffi = require("ffi")

ffi.cdef[[
  long long factorial(long long n);
  const char* greet(const char* name);
  const char* repeat_string(const char* s, long long n);
  void shout_it(const char* s);
  int maybe();
  const char* sort_json_array(const char* json);
]]

local lib = ffi.load(os.getenv("LD_LIBRARY_PATH") .. "/example.so")

for n = 1, 10 do
  print(string.format("%d! = %d", n, tonumber(lib.factorial(n))))
end

print(ffi.string(lib.greet("World")))
print(ffi.string(lib.repeat_string("ha", 3)))

lib.shout_it("hello from yamlscript")

local m = lib.maybe()
print(string.format("maybe: %s", m ~= 0 and "true" or "false"))

print("sorted: " .. ffi.string(lib.sort_json_array("[3,1,4,1,5,9,2,6]")))
