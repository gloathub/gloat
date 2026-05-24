#!/usr/bin/env bash

# Test java.lang.Character interop in a gloat-compiled binary. Verifies that
# Character/* statics flow from glojure's rewrite, through the
# javacompat/character bridge, into gojava. Exercises both `\c` char-literal
# arguments (lang.Char) and integer code-point arguments.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Character/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/character-bin
fixture=$PROJECT_ROOT/test/fixtures/character.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds character fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check is-digit     'true'  'Character/isDigit \5'
check is-digit-no  'false' 'Character/isDigit \a -> false'
check is-letter    'true'  'Character/isLetter \x'
check is-ld        'true'  'Character/isLetterOrDigit \3'
check is-upper     'true'  'Character/isUpperCase \X'
check is-lower     'false' 'Character/isLowerCase \X -> false'
check is-ws        'true'  'Character/isWhitespace \space'
check is-ws-tab    'true'  'Character/isWhitespace \tab'
check is-spacechar 'true'  'Character/isSpaceChar \space'
check is-alpha     'true'  'Character/isAlphabetic \z'
check to-upper     'A'     'Character/toUpperCase \a'
check to-lower     'z'     'Character/toLowerCase \Z'
check to-string    'k'     'Character/toString \k'
check digit-hex    '15'    'Character/digit \f 16'
check digit-oob    '-1'    'Character/digit \z 10 -> -1'
check for-digit    'a'     'Character/forDigit 10 16 -> a'
check for-digit-9  '9'     'Character/forDigit 9 10'
check numeric      '7'     'Character/getNumericValue \7'
check compare-lt   '-1'    'Character/compare \a \b'
check compare-eq   '0'     'Character/compare \a \a'
check MIN_RADIX    '2'     'Character/MIN_RADIX'
check MAX_RADIX    '36'    'Character/MAX_RADIX'
check ctor         'W'     '(Character. \W)'
check fq-isdigit   'true'  'java.lang.Character/isDigit'

done-testing
