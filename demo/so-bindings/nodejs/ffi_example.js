const koffi = require('koffi');

const lib = koffi.load('example.so');

const factorial = lib.func('long long factorial(long long n)');
const greet = lib.func('const char* greet(const char* name)');
const repeat_string = lib.func('const char* repeat_string(const char* s, long long n)');
const shout_it = lib.func('void shout_it(const char* s)');
const maybe = lib.func('int maybe()');
const sort_json_array = lib.func('const char* sort_json_array(const char* json)');

for (let n = 1; n <= 10; n++) {
  console.log(`${n}! = ${factorial(n)}`);
}

console.log(greet("World"));
console.log(repeat_string("ha", 3));

shout_it("hello from yamlscript");

console.log(`maybe: ${maybe() !== 0}`);

console.log(`sorted: ${sort_json_array("[3,1,4,1,5,9,2,6]")}`);
