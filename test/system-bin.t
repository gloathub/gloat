#!/usr/bin/env bash

# Test java.lang.System interop in a gloat-compiled binary. Verifies that
# System/* symbols rewritten by glojure's rewrite-core flow through the
# javacompat bridge into gojava and produce JVM-faithful results.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow System/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/system-bin
fixture=$PROJECT_ROOT/test/fixtures/system.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds system fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

# (op expected description)
check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check millis-pos 'true'      'currentTimeMillis is positive'
check nanos-pos  'true'      'nanoTime is positive'
check env-set    'true'      'getenv PATH returns non-nil'
check env-unset  'nil'       'getenv of unset returns nil'
check env-all    'true'      'getenv (0-arg) returns map containing PATH'
check prop-home  'true'      'getProperty user.home is set'
check prop-unset 'nil'       'getProperty unknown returns nil'
check prop-or    'fallback'  'getProperty 2-arg returns default when unset'
check prop-set   'v1'        'setProperty then getProperty round-trips'
check prop-clear 'v1 nil'    'clearProperty returns old value, then unset'
check line-sep   '1'         'lineSeparator is one char on Linux'
check stdout     'via stdout' '.println System/out writes to stdout'
check gc         'ok'        'gc returns nil'

# stderr test: verify it goes to fd 2, not fd 1
try "$bin stderr 2>&1 1>/dev/null"
is "$got" 'via stderr' '.println System/err writes to stderr'

# env passthrough: verify a set var is visible
try "GLOAT_TEST_KEY=hello $bin env-value"
is "$got" 'hello' 'getenv reads live environment'

done-testing
