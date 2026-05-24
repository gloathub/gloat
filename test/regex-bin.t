#!/usr/bin/env bash

# Test java.util.regex.Pattern + Matcher interop in a gloat-compiled binary.
# Verifies Pattern/* statics, constants, the (Pattern. ...) constructor, and
# Matcher instance methods reached through reflective FieldOrMethod dispatch.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Pattern/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/regex-bin
fixture=$PROJECT_ROOT/test/fixtures/regex.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds regex fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check matches-true   'true'                 'Pattern/matches whole input matches'
check matches-false  'false'                'Pattern/matches partial does not'
check quote          'a\.b\*c'              'Pattern/quote escapes meta'
check ci-const       '2'                    'Pattern/CASE_INSENSITIVE constant'
check multi-const    '8'                    'Pattern/MULTILINE constant'
check dotall-const   '32'                   'Pattern/DOTALL constant'
check literal-const  '16'                   'Pattern/LITERAL constant'
check find           'true'                 'Matcher .find returns true'
check group0         '42-foo'               'Matcher .group (no arg) yields whole match'
check group1         '42'                   'Matcher .group 1 yields first capture'
check group2         'foo'                  'Matcher .group 2 yields second capture'
check start          '4'                    'Matcher .start indexes first match'
check end            '7'                    'Matcher .end indexes after match'
check matches        'true'                 'Matcher .matches whole input'
check lookingat      'true'                 'Matcher .lookingAt prefix match'
check groupcount     '2'                    'Matcher .groupCount counts captures'
check split          '[foo bar baz]'        'Pattern .split on whitespace'
check split-limit    '[a b c   ]'           'Pattern .splitLimit -1 keeps trailing empties'
check ci-flag        'true'                 'Pattern/compile flag CASE_INSENSITIVE'
check replaceall     'aN bN cN'             'Matcher .replaceAll replaces every match'
check replacefirst   'aN b22 c333'          'Matcher .replaceFirst only first match'
check fq-compile     'xxxx'                 'java.util.regex.Pattern/compile fully qualified'
check ctor           'abcabc'               '(Pattern. regex) constructor sugar'
check pattern-str    '\d+'                  'Pattern .pattern returns source'

done-testing
