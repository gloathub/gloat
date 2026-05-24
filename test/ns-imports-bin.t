#!/usr/bin/env bash

# Test that (ns-imports *ns*) returns the auto-imported host classes in a
# gloat-compiled binary. Mirrors real Clojure's auto-import of java.lang.*
# (plus our other registered packages) so libraries that introspect their
# namespace's imports map see a populated table.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow ns-imports binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/ns-imports-bin
fixture=$PROJECT_ROOT/test/fixtures/ns-imports.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds ns-imports fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check count-pos    'true' '(count (ns-imports *ns*)) > 0'
check has-math     'true' '(ns-imports *ns*) contains Math'
check has-system   'true' '(ns-imports *ns*) contains System'
check has-thread   'true' '(ns-imports *ns*) contains Thread'
check has-string   'true' '(ns-imports *ns*) contains String'
check has-pattern  'true' '(ns-imports *ns*) contains Pattern'
check has-uuid     'true' '(ns-imports *ns*) contains UUID'
check has-instant  'true' '(ns-imports *ns*) contains Instant'
check twelve       'true' '(ns-imports *ns*) has all 12 host classes'

# Host-class values must render as their FQ Java name (matching JVM
# Clojure's `{Math java.lang.Math, ...}` form), not as the underlying
# Go type via the `#object[...]` catch-all.
check show-math    'java.lang.Math' '(str Math) renders as java.lang.Math'
check show-uuid    'java.util.UUID' '(str UUID) renders as java.util.UUID'
check pr-math      'java.lang.Math' '(pr Math) renders as java.lang.Math'

done-testing
