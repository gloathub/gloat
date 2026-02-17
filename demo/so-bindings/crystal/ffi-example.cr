require "json"

@[Link(ldflags: "-L#{__DIR__}/.. -l:example.so")]
lib Example
  fun factorial(n : Int64) : Int64
  fun greet(name : LibC::Char*) : LibC::Char*
  fun repeat_string(s : LibC::Char*, n : Int64) : LibC::Char*
  fun shout_it(s : LibC::Char*) : Void
  fun maybe() : Int32
  fun sort_json_array(json : LibC::Char*) : LibC::Char*
end

(1..10).each do |n|
  puts "#{n}! = #{Example.factorial(n)}"
end

puts String.new(Example.greet("World"))
puts String.new(Example.repeat_string("ha", 3))

Example.shout_it("hello from yamlscript")

puts "maybe: #{Example.maybe() != 0}"

data = [3, 1, 4, 1, 5, 9, 2, 6].to_json
puts "sorted: #{String.new(Example.sort_json_array(data))}"
