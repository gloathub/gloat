#!/usr/bin/env bash

# Test java.lang.Thread/sleep interop in a gloat-compiled binary. Verifies
# the millisecond and (millis, nanos) overloads, the fully qualified
# java.lang.Thread/sleep form, and that the call actually blocks.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Thread/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/thread-bin
fixture=$PROJECT_ROOT/test/fixtures/thread.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds thread fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check sleep-zero  'ok'   'Thread/sleep 0 returns'
check sleep-short 'ok'   'Thread/sleep 5 returns'
check sleep-nanos 'ok'   'Thread/sleep millis,nanos returns'
check fq-sleep    'ok'   'java.lang.Thread/sleep resolves through bridge'
check elapsed-ge  'true' 'Thread/sleep actually blocks for the requested duration'

done-testing
