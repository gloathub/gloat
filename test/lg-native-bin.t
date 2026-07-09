#!/usr/bin/env bash

# Test the LG engine end to end: -ELG compiles to a native binary via
# let-go's AOT Go lowering (lg-compile -> lg -c bundle -> go build).
# Needs a let-go source checkout next to the gloat repo and, on first
# run, network for Go module downloads.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow LG native binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/hello-LG
fixture=$PROJECT_ROOT/test/fixtures/hello.ys

try "gloat -q -ELG -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -ELG -o builds a native binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

try "$bin"
is "$got" "Hello, World!" "LG native binary runs standalone"

try "$bin Gloat"
is "$got" "Hello, Gloat!" "LG native binary takes args"

try "gloat -ELG --run $fixture -- Gloat"
is "$rc" 0 "'gloat -ELG --run' exits 0"
is "$got" "Hello, Gloat!" "'gloat -ELG --run' passes program args"

printf '(ns boom.core)\n(defn -main [& _] (throw (ex-info "boom" {})))' \
  > "$TMP/boom-LG.clj"
try "gloat -ELG --run $TMP/boom-LG.clj"
is "$rc" 1 "'gloat -ELG --run' propagates failure exit code"

done-testing
