#!/usr/bin/env bash

# Test java.lang.Integer and java.lang.Long interop in a gloat-compiled
# binary. Verifies that Integer/* and Long/* symbols rewritten by
# glojure's rewrite-core flow through the javacompat bridge into gojava
# and produce JVM-faithful results.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Integer/Long binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/integer-long-bin
fixture=$PROJECT_ROOT/test/fixtures/integer-long.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds integer-long fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check parse-int       '42'          'Integer/parseInt decimal'
check parse-int-radix '10'          'Integer/parseInt with radix 2'
check parse-int-hex   '255'         'Integer/parseInt with radix 16'
check parse-long      '9999999999'  'Long/parseLong handles values >int32'

check int-max  '2147483647'           'Integer/MAX_VALUE is JVM int max'
check int-min  '-2147483648'          'Integer/MIN_VALUE is JVM int min'
check long-max '9223372036854775807'  'Long/MAX_VALUE is JVM long max'
check long-min '-9223372036854775808' 'Long/MIN_VALUE is JVM long min'

check to-bin      '101010' 'Integer/toBinaryString'
check to-hex      'ff'     'Integer/toHexString'
check to-oct      '10'     'Integer/toOctalString'
check long-to-hex '1000'   'Long/toHexString'

check value-of-int '7' 'Integer/valueOf accepts int'
check value-of-str '7' 'Integer/valueOf parses string'

check ctor-int  '5'     '(Integer. n) rewrites to valueOf'
check ctor-str  '5'     '(Integer. "s") parses'
check ctor-long '12345' '(Long. n) rewrites to valueOf'

check bit-count       '8'  'Integer/bitCount 0xFF'
check leading-zeros   '31' 'Integer/numberOfLeadingZeros 1'
check trailing-zeros  '3'  'Integer/numberOfTrailingZeros 8'
check long-bit-count  '24' 'Long/bitCount 0xFFFFFF'

check imax '7'  'Integer/max'
check lmin '3'  'Long/min'

check signum-neg  '-1' 'Integer/signum negative'
check signum-pos  '1'  'Integer/signum positive'
check signum-zero '0'  'Integer/signum zero'

done-testing
