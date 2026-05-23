#!/usr/bin/env bash

# Test java.lang.Math interop in a gloat-compiled binary. Verifies that
# Math/* symbols rewritten by glojure's rewrite-core flow through the
# javacompat bridge into gojava and produce JVM-faithful results.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Math/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/math-bin
fixture=$PROJECT_ROOT/test/fixtures/math.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds math fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

# (op expected description)
check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check pi        '3.141592653589793'  'Math/PI'
check e         '2.718281828459045'  'Math/E'
check sqrt      '12.0'               'Math/sqrt 144 -> 12.0'
check pow       '1024.0'             'Math/pow 2 10 -> 1024.0'
check abs-long  '42'                 'Math/abs of long stays long'
check abs-dbl   '3.5'                'Math/abs of double stays double'
check floor     '2.0'                'Math/floor 2.7 -> 2.0'
check ceil      '3.0'                'Math/ceil 2.3 -> 3.0'
check round-up  '3'                  'Math/round 2.5 rounds toward +Inf'
check round-dn  '-2'                 'Math/round -2.5 rounds toward +Inf (JVM)'
check floordiv  '-4'                 'Math/floorDiv -7 2 -> -4'
check floormod  '1'                  'Math/floorMod -7 2 -> 1'
check hypot     '5.0'                'Math/hypot 3 4 -> 5.0'
check atan2     '0.7853981633974483' 'Math/atan2 1 1 -> pi/4'
check tor       '3.141592653589793'  'Math/toRadians 180 -> pi'

done-testing
