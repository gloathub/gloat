#!/usr/bin/env bash

# Test java.lang.Boolean interop in a gloat-compiled binary. Verifies that
# Boolean/* statics flow from glojure's rewrite, through the javacompat/boolean
# bridge, into gojava.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Boolean/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/boolean-bin
fixture=$PROJECT_ROOT/test/fixtures/boolean.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds boolean fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check parse-true   'true'  'Boolean/parseBoolean "true"'
check parse-TRUE   'true'  'Boolean/parseBoolean "TRUE" (case insensitive)'
check parse-yes    'false' 'Boolean/parseBoolean "yes" -> false'
check parse-empty  'false' 'Boolean/parseBoolean "" -> false'
check valueof-str  'true'  'Boolean/valueOf "True"'
check valueof-bool 'true'  'Boolean/valueOf true'
check tostring     'true'  'Boolean/toString true'
check tostring-f   'false' 'Boolean/toString false'
check compare-lt   '-1'    'Boolean/compare false true -> -1'
check compare-eq   '0'     'Boolean/compare true true -> 0'
check and          'false' 'Boolean/logicalAnd true false'
check or           'true'  'Boolean/logicalOr true false'
check xor          'false' 'Boolean/logicalXor true true'
check TRUE         'true'  'Boolean/TRUE constant'
check FALSE        'false' 'Boolean/FALSE constant'
check ctor-str     'true'  '(Boolean. "true") -> valueOf'
check ctor-bool    'false' '(Boolean. false)'
check fq-parse     'true'  'java.lang.Boolean/parseBoolean'

done-testing
