#!/usr/bin/env bash

# Test java.lang.Double interop in a gloat-compiled binary. Verifies that
# Double/* statics flow from glojure's rewrite, through the javacompat/double
# bridge, into gojava.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Double/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/double-bin
fixture=$PROJECT_ROOT/test/fixtures/double.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds double fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

# (op expected description)
check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check parse          '3.14'                    'Double/parseDouble "3.14"'
check parse-neg      '-2.5'                    'Double/parseDouble "-2.5"'
check parse-inf      'Infinity'                'Double/parseDouble "Infinity"'
check valueof-str    '1.5'                     'Double/valueOf "1.5"'
check valueof-num    '2.0'                     'Double/valueOf 2.0'
check tostring       '3.14'                    'Double/toString 3.14'
check tostring-big   '1.7976931348623157E308'  'Double/toString MAX_VALUE (JVM-style E)'
check tostring-nan   'NaN'                     'Double/toString NaN'
check tostring-inf   'Infinity'                'Double/toString +Inf'
check isnan          'true'                    'Double/isNaN NaN'
check isnan-no       'false'                   'Double/isNaN 1.0'
check isinf          'true'                    'Double/isInfinite POSITIVE_INFINITY'
check isfinite       'true'                    'Double/isFinite 1.5'
check isfinite-no    'false'                   'Double/isFinite NaN'
check max            '7.0'                     'Double/max 3.0 7.0'
check min            '3.0'                     'Double/min 3.0 7.0'
check sum            '4.0'                     'Double/sum 1.5 2.5'
check compare-lt     '-1'                      'Double/compare 1.0 2.0'
check compare-eq     '0'                       'Double/compare 1.0 1.0'
check bits-roundtrip '3.14'                    'doubleToLongBits roundtrip'
check ctor           '1.25'                    '(Double. "1.25") -> valueOf'
check ctor-num       '4.0'                     '(Double. 4.0) -> valueOf'
check fq-parse       '0.5'                     'java.lang.Double/parseDouble'

done-testing
