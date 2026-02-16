#!/usr/bin/env bash

# Test auto-detection of file extensions

source "$(dirname "${BASH_SOURCE[0]}")/init"

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "$TEST_DIR/.." && pwd)
GLOAT_BIN=$PROJECT_ROOT/bin/gloat
EXAMPLE_DIR=$PROJECT_ROOT/demo/yamlscript

cd "$PROJECT_ROOT" || bail-out "Cannot cd to project root"
source .rc || bail-out "Cannot source .rc"

# Auto-detect .ys extension with -t bb
try "$GLOAT_BIN $EXAMPLE_DIR/factorial -t bb"
is "$rc" 0 "factorial (without .ys) compiles to bb format"
has "$got" "defn factorial" "bb output contains factorial function"

# Auto-detect .ys extension with -t clj
try "$GLOAT_BIN $EXAMPLE_DIR/factorial -t clj"
is "$rc" 0 "factorial (without .ys) compiles to clj format"
has "$got" "(ns " "clj output contains namespace declaration"

if [[ ${RUN_SLOW_TESTS:-} ]]; then
  # Auto-detect .ys extension with -o binary
  TMP_BIN=$(mktemp)
  rm "$TMP_BIN"  # Remove so gloat can create it
  try "$GLOAT_BIN $EXAMPLE_DIR/factorial -o $TMP_BIN"
  is "$rc" 0 "factorial (without .ys) compiles to binary"
  ok "$([[ -x $TMP_BIN ]])" "binary is executable"

  try "$TMP_BIN 5"
  is "$rc" 0 "binary runs successfully"
  is "$got" "5! -> 120" "binary produces correct output"
  rm -f "$TMP_BIN"

  # Test 4: Auto-detect with --run
  try "$GLOAT_BIN --run $EXAMPLE_DIR/factorial -- 5"
  is "$rc" 0 "factorial (without .ys) runs with --run"
  is "$got" "5! -> 120" "--run produces correct output"
fi

# Error when file doesn't exist
try "$GLOAT_BIN $EXAMPLE_DIR/nonexistent -t clj"
is "$rc" 1 "nonexistent file fails"
has "$got" "does not exist" "shows proper error message"

# Explicit .ys extension still works
if [[ ${RUN_SLOW_TESTS:-} ]]; then
  try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t clj"
  is "$rc" 0 "explicit .ys extension works"
  has "$got" "(ns " "clj output contains namespace"
fi

# Files with dots in name handled correctly
try "$GLOAT_BIN $EXAMPLE_DIR/99-bottles-of-beer.ys -t clj"
is "$rc" 0 "files with dots in name compile correctly"
has "$got" "(ns " "clj output contains namespace"

done-testing
