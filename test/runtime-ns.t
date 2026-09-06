#!/usr/bin/env bash

# Test that a generated main.go links and requires only the runtime
# namespaces the program reaches, and everything with GLOAT_YS_RUNTIME=all.

source "$(dirname "${BASH_SOURCE[0]}")/init"

# This test covers the default main.go template.
# -Xprune builds main.go from main-prune.go and picks its namespaces in
# deep-prune, so a GLOAT_X_PRUNE=1 test run must not redirect it there.
unset GLOAT_X_PRUNE

fixture=$PROJECT_ROOT/test/fixtures/runtime-ns.clj
ys_fixture=$PROJECT_ROOT/test/fixtures/hello.ys

try "gloat -q -o $TMP/used/ $fixture 2>&1"
is "$rc" 0 'gloat generates a module for the runtime-ns fixture'
main=$TMP/used/main.go
try "cat $main"
has "$got" '_ "github.com/gloathub/ys-v0-glj/ys/v0/json"' \
  'main.go imports the ys.v0.json loader the program uses'
has "$got" '_ "github.com/gloathub/ys-v0-glj/ys/v0/util"' \
  'main.go imports ys.v0.util through ys.v0.json'
has "$got" '_ "github.com/glojurelang/glojure/pkg/stdlib/clojure/string"' \
  'main.go imports the clojure.string loader the program uses'
has "$got" 'require.Invoke(lang.NewSymbol("ys.v0.util"))' \
  'main.go requires ys.v0.util'
hasnt "$got" 'ys-v0-glj/runtime' \
  'main.go does not load the whole runtime'
hasnt "$got" 'ys/v0/std' \
  'main.go leaves out ys.v0.std'
try "grep -c 'require.Invoke(lang.NewSymbol(\"ys.v0.util\"))' $main"
is "$got" 1 'ys.v0.util is required once'

try "GLOAT_YS_RUNTIME=all gloat -q -o $TMP/all/ $fixture 2>&1"
is "$rc" 0 'gloat generates a module with GLOAT_YS_RUNTIME=all'
try "cat $TMP/all/main.go"
has "$got" '_ "github.com/gloathub/ys-v0-glj/ys/v0/std"' \
  'GLOAT_YS_RUNTIME=all links ys.v0.std'
has "$got" '_ "github.com/glojurelang/glojure/pkg/stdlib/clojure/walk"' \
  'GLOAT_YS_RUNTIME=all links clojure.walk from the stdlib'

try "gloat -q -o $TMP/ys/ $ys_fixture 2>&1"
is "$rc" 0 'gloat generates a module for a YAMLScript program'
try "cat $TMP/ys/main.go"
has "$got" '_ "github.com/gloathub/ys-v0-glj/ys/v0/std"' \
  'a YAMLScript program links ys.v0.std'
has "$got" '_ "github.com/gloathub/ys-v0-glj/ys/v0/global"' \
  'a YAMLScript program links ys.v0.global'

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow runtime-ns binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/runtime-ns-bin
try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 'gloat builds the runtime-ns fixture binary'
try "$bin a b"
is "$got" '{"args":"a b"}' 'the binary runs with the trimmed runtime'

done-testing
