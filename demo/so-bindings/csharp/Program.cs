using System.Runtime.InteropServices;
using System.Text.Json;

const string lib = "example";

// factorial(int) -> int
[DllImport(lib)]
static extern long factorial(long n);

// greet(str) -> str
[DllImport(lib)]
static extern IntPtr greet([MarshalAs(UnmanagedType.LPUTF8Str)] string name);

// repeat_string(str, int) -> str
[DllImport(lib)]
static extern IntPtr repeat_string(
    [MarshalAs(UnmanagedType.LPUTF8Str)] string s, long n);

// shout_it(str) -> void
[DllImport(lib)]
static extern void shout_it([MarshalAs(UnmanagedType.LPUTF8Str)] string s);

// maybe() -> bool (as int)
[DllImport(lib)]
static extern int maybe();

// sort_json_array(str) -> str
[DllImport(lib)]
static extern IntPtr sort_json_array(
    [MarshalAs(UnmanagedType.LPUTF8Str)] string json);

for (int n = 1; n <= 10; n++)
    Console.WriteLine($"{n}! = {factorial(n)}");

var greetResult = Marshal.PtrToStringUTF8(greet("World"));
Console.WriteLine(greetResult);

var repeatResult = Marshal.PtrToStringUTF8(repeat_string("ha", 3));
Console.WriteLine(repeatResult);

shout_it("hello from yamlscript");

var maybeResult = maybe();
Console.WriteLine($"maybe: {(maybeResult != 0 ? "true" : "false")}");

var data = JsonSerializer.Serialize(new[] { 3, 1, 4, 1, 5, 9, 2, 6 });
var sortResult = Marshal.PtrToStringUTF8(sort_json_array(data));
Console.WriteLine($"sorted: {sortResult}");
