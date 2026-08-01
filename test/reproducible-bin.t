#!/usr/bin/env bash

# Native outputs built from identical inputs must not retain Gloat's random
# temporary build directory or otherwise vary between invocations.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping reproducible binary builds. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

fixture=$PROJECT_ROOT/test/fixtures/math.clj
first=$TMP/reproducible-one
second=$TMP/reproducible-two

try "gloat -q -o '$first' '$fixture' 2>&1"
is "$rc" 0 "first native build succeeds"

try "gloat -q -o '$second' '$fixture' 2>&1"
is "$rc" 0 "second native build succeeds"

try "cmp '$first' '$second'"
is "$rc" 0 "identical inputs produce byte-identical binaries"

done-testing
