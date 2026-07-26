#!/usr/bin/env bash

# Runtime eval must be able to resolve Go host functions used by macro
# expansions when the goimports extension is enabled.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow runtime-eval binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/runtime-eval-bin
fixture=$PROJECT_ROOT/test/fixtures/runtime-eval.clj

try "gloat -q -Xgoimports -o $bin $fixture 2>&1"
is "$rc" 0 "gloat builds a runtime-eval binary with goimports"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

try "$bin"
is "$rc" 0 "runtime eval of ns succeeds"
is "$got" "fib.core" "runtime eval preserves the namespace change"

done-testing
