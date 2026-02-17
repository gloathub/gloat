#!/usr/bin/env bash

# Tests for compiling and running all demo programs as native binaries

source "$(dirname "${BASH_SOURCE[0]}")/init"

source .rc
cd demo/ || bail-out "Cannot cd to demo/"

# Clean up binaries on exit
trap 'make -s clean >/dev/null' EXIT

# Helper function to test a binary
test-bin() {
  local name=$1
  local args=$2
  local expected=$3

  note "Testing $name"

  if [[ -n $args ]]; then
    try "make -s run-bin FILE=yamlscript/$name.ys $args 2>/dev/null"
  else
    try "make -s run-bin FILE=yamlscript/$name.ys 2>/dev/null"
  fi

  is "$rc" 0 "$name exits 0"

  if [[ -n $expected ]]; then
    has "$got" "$expected" "$name output contains '$expected'"
  fi
}

# Test all 28 examples
if [[ ${RUN_SLOW_TESTS:-} ]]; then
  test-bin "100-doors" "" "Open doors"
  test-bin "99-bottles-of-beer" "a1=3" "bottles of beer"
  test-bin "array-length" "" "Length of"
  test-bin "best-shuffle" "a1=abracadabra" "abracadabra"
  test-bin "dragon-curve" "a1=63" ""
  test-bin "even-or-odd" "a1=42" "42 is even"
  test-bin "factorial" "a1=5" "5! -> 120"
  test-bin "fibonacci-sequence" "a1=10" "0"
  test-bin "fizzbuzz" "a1=16" "FizzBuzz"
  test-bin "floyds-triangle" "a1=5" ""
  test-bin "function-definition" "a1=2 a2=3 a3=4" "multiply(2, 3, 4)"
  test-bin "greatest-common-divisor" "a1=42 a2=63" "21"
  test-bin "hello-world-text" "" "Hello, world!"
  test-bin "integer-comparison" "a1=5 a2=10" "less than"
  test-bin "jortsort" "a1=1 a2=2 a3=3" "sorted"
  test-bin "leap-year" "a1=2024" "leap year"
  test-bin "luhn-test-of-credit-card-numbers" "a1=49927398716" "valid"
  test-bin "next-highest-int-from-digits" "a1=12453" "12534"
  test-bin "one-dimensional-cellular-automata" "a1=5" "#"
  test-bin "palindrome-detection" "a1=12321" "palindrome"
  test-bin "sieve-of-eratosthenes" "a1=25" "2"
  test-bin "sparkline-in-unicode" "a1=1 a2=2 a3=3" ""
  test-bin "tokenize-a-string" "a1=Hello,How,Are,You,Today" "Hello.How.Are.You.Today"
  test-bin "validate-international-securities-identification-number" "a1=US0378331005" "valid"
  test-bin "van-der-corput-sequence" "a1=2" "0."
  test-bin "weird-numbers" "a1=100" "70"
  test-bin "xiaolin-wus-line-algorithm" "" "plot"
  test-bin "yellowstone-sequence" "a1=10" "1 2 3"
else
  pass 'Skpping these slow tests. Try RUN_SLOW_TESTS=1.'
fi

done-testing
