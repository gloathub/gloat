use NativeCall;

constant LIB = %*ENV<LD_LIBRARY_PATH> ~ '/example.so';

sub factorial(int64 --> int64)
  is native(LIB) {*}

sub greet(Str --> Str)
  is native(LIB) {*}

sub repeat_string(Str, int64 --> Str)
  is native(LIB) {*}

sub shout_it(Str)
  is native(LIB) {*}

sub maybe(--> int32)
  is native(LIB) {*}

sub sort_json_array(Str --> Str)
  is native(LIB) {*}

for 1..10 -> $n {
    say "$n! = {factorial($n)}";
}

say greet("World");
say repeat_string("ha", 3);

shout_it("hello from yamlscript");

say "maybe: {maybe() != 0 ?? 'true' !! 'false'}";

say "sorted: {sort_json_array('[3,1,4,1,5,9,2,6]')}";
