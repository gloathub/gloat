require 'fiddle'
require 'fiddle/import'

module Example
  extend Fiddle::Importer
  dlload 'example.so'

  extern 'long long factorial(long long)'
  extern 'const char* greet(const char*)'
  extern 'const char* repeat_string(const char*, long long)'
  extern 'void shout_it(const char*)'
  extern 'int maybe()'
  extern 'const char* sort_json_array(const char*)'
end

(1..10).each do |n|
  puts "#{n}! = #{Example.factorial(n)}"
end

puts Example.greet("World")
puts Example.repeat_string("ha", 3)

Example.shout_it("hello from yamlscript")

puts "maybe: #{Example.maybe() != 0}"

puts "sorted: #{Example.sort_json_array('[3,1,4,1,5,9,2,6]')}"
