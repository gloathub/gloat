#!/usr/bin/env bash

# Test java.lang.String interop in a gloat-compiled binary. Verifies
# that String/* statics and (.method s) instance forms flow from
# glojure's rewrite, through the javacompat/string bridge, into gojava.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow String/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/string-bin
fixture=$PROJECT_ROOT/test/fixtures/string.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds string fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

# (op expected description)
check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check len          '5'                  '(.length "hello") -> 5'
check len-utf8     '5'                  '(.length "naïve") -> 5 UTF-16 units'
check empty?       'true'               '(.isEmpty "") -> true'
check blank?       'true'               '(.isBlank "  \t  ") -> true'
check upper        'HELLO'              '(.toUpperCase "hello")'
check lower        'hello'              '(.toLowerCase "HELLO")'
check trim         '[hi]'               '(.trim "  hi  ") -> "hi"'
check strip        '[hi]'               '(.strip "   hi   ") -> "hi"'
check substr       'world'              '(.substring "hello world" 6)'
check substr-range 'hello'              '(.substring "hello world" 0 5)'
check starts       'true'               '(.startsWith "hello world" "hello")'
check ends         'true'               '(.endsWith "hello world" "world")'
check contains     'true'               '(.contains "hello world" "lo wo")'
check indexof      '6'                  '(.indexOf "hello world" "world")'
check lastindexof  '4'                  '(.lastIndexOf "ababab" "ab")'
check equals       'true'               '(.equals "abc" "abc")'
check equals-ic    'true'               '(.equalsIgnoreCase "ABC" "abc")'
check compareto    '-1'                 '(.compareTo "abc" "abd") -> -1'
check concat       'foobar'             '(.concat "foo" "bar")'
check repeat       'ababab'             '(.repeat "ab" 3)'
check replace      'fOObar'             '(.replace "foobar" "oo" "OO")'
check replaceall   'aXbXcX'             '(.replaceAll regex digits -> X)'
check replacefirst 'aXb2c3'             '(.replaceFirst regex digit -> X)'
check matches      'true'               '(.matches "abc123" "[a-z]+[0-9]+")'
check split        '[a b c  e]'         '(.split "a,b,c,,e" ",") keeps inner empties'
check split-limit  '[a b,c,d]'          '(.split "a,b,c,d" "," 2) honors limit'
check hash         '99162322'           '(.hashCode "hello") JVM algorithm'
check format       'x=42'               'String/format "%s=%d"'
check format-pct   '3.14%'              'String/format "%.2f%%"'
check join         'a-b-c'              'String/join with vector arg'
check join-empty   '[]'                 'String/join with empty seq'
check valueof-nil  'null'               'String/valueOf nil -> "null"'
check valueof-bool 'true'               'String/valueOf true'
check valueof-int  '42'                 'String/valueOf 42'
check ctor         'hello'              '(String. "hello") -> "hello"'
check fq-format    'x=7'                'java.lang.String/format works too'
check chain        'HELLO WORLD'        'chained .trim / .toUpperCase'

done-testing
