#!/usr/bin/env bash

# Test reading from stdin with -t format

source "$(dirname "${BASH_SOURCE[0]}")/init"

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "$TEST_DIR/.." && pwd)
GLOAT_BIN=$PROJECT_ROOT/bin/gloat

cd "$PROJECT_ROOT" || bail-out "Cannot cd to project root"
source .rc || bail-out "Cannot source .rc"

INPUT='(defn -main [] (println "Hello"))'

# Test 1: gloat -t clj - <<<input
try "echo '$INPUT' | $GLOAT_BIN -t clj -"
is "$rc" 0 "-t clj - exits 0"
has "$got" "defn -main" "-t clj - produces clj output"

# Test 2: gloat -t clj <<<input (no explicit -)
try "echo '$INPUT' | $GLOAT_BIN -t clj"
is "$rc" 0 "-t clj (implicit stdin) exits 0"
has "$got" "defn -main" "-t clj (implicit stdin) produces clj output"

done-testing
