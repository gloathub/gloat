use strict;
use warnings;
use FFI::Platypus 2.00;

my $ffi = FFI::Platypus->new(
    api => 2,
    lib => 'example.so',
);

$ffi->attach(factorial => ['sint64'] => 'sint64');
$ffi->attach(greet => ['string'] => 'string');
$ffi->attach(repeat_string => ['string', 'sint64'] => 'string');
$ffi->attach(shout_it => ['string'] => 'void');
$ffi->attach(maybe => [] => 'int');
$ffi->attach(sort_json_array => ['string'] => 'string');

for my $n (1..10) {
    printf "%d! = %d\n", $n, factorial($n);
}

print greet("World"), "\n";
print repeat_string("ha", 3), "\n";

shout_it("hello from yamlscript");

printf "maybe: %s\n", maybe() ? "true" : "false";

printf "sorted: %s\n", sort_json_array("[3,1,4,1,5,9,2,6]");
