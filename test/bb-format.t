#!/usr/bin/env bash

# Test -t bb format can be run with babashka

source "$(dirname "${BASH_SOURCE[0]}")/init"

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "$TEST_DIR/.." && pwd)
GLOAT_BIN=$PROJECT_ROOT/bin/gloat
EXAMPLE_DIR=$PROJECT_ROOT/example/yamlscript
BB_BIN=$PROJECT_ROOT/.cache/.local/bin/bb

cd "$PROJECT_ROOT" || bail-out "Cannot cd to project root"
source .rc || bail-out "Cannot source .rc"

# Check if babashka is available
if [[ ! -x $BB_BIN ]]; then
  skip-all "Babashka not found at $BB_BIN (run 'make shell' first)"
fi

# Test 1: bb format can be executed with babashka
TMP_BB=$(mktemp --suffix=.bb)
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t bb"
is "$rc" 0 "bb format compiles"
echo "$got" > "$TMP_BB"

try "$BB_BIN $TMP_BB 5"
is "$rc" 0 "bb format executes with babashka"
is "$got" "5! -> 120" "bb format produces correct output"

# Test 2: bb format with multiple arguments
try "$BB_BIN $TMP_BB 5 10"
is "$rc" 0 "bb format handles multiple arguments"
has "$got" "5! -> 120" "first argument output correct"
has "$got" "10! -> 3628800" "second argument output correct"

# Test 3: bb format with no arguments
try "$BB_BIN $TMP_BB"
is "$rc" 0 "bb format runs with no arguments"

rm -f "$TMP_BB"

# Test 4: bb format can be saved to file with -o
TMP_BB2=$(mktemp --suffix=.bb)
rm "$TMP_BB2"  # Remove so gloat can create it
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -o $TMP_BB2"
is "$rc" 0 "bb format saves to file with -o"
ok "$([[ -f $TMP_BB2 ]])" "bb file exists"

try "$BB_BIN $TMP_BB2 7"
is "$rc" 0 "saved bb file runs"
is "$got" "7! -> 5040" "saved bb file produces correct output"
rm -f "$TMP_BB2"

# Test 5: bb format without extension auto-detects
TMP_BB3=$(mktemp --suffix=.bb)
try "$GLOAT_BIN $EXAMPLE_DIR/factorial -t bb"
is "$rc" 0 "auto-detection works with bb format"
echo "$got" > "$TMP_BB3"

try "$BB_BIN $TMP_BB3 3"
is "$rc" 0 "auto-detected bb file runs"
is "$got" "3! -> 6" "auto-detected bb format produces correct output"
rm -f "$TMP_BB3"

# Test 6: bb format already includes runner line
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t bb"
is "$rc" 0 "bb format compiles for runner line check"
LAST_LINE=$(echo "$got" | tail -1)
ok "$([[ $LAST_LINE == *'(apply -main *command-line-args*)'* ]])" \
  "bb format includes (apply -main *command-line-args*)"

# Test 7: --run with -t bb executes with babashka
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t bb -r -- 5"
is "$rc" 0 "-r with -t bb executes successfully"
is "$got" "5! -> 120" "-r with -t bb produces correct output"

# Test 8: --run with -t bb and multiple arguments
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t bb -r -- 5 10"
is "$rc" 0 "-r with -t bb handles multiple arguments"
has "$got" "5! -> 120" "first argument correct with -r -t bb"
has "$got" "10! -> 3628800" "second argument correct with -r -t bb"

# Test 9: --run with -t bb and no arguments
try "$GLOAT_BIN $EXAMPLE_DIR/factorial.ys -t bb -r"
is "$rc" 0 "-r with -t bb runs with no arguments"

# Test 10: --run with -t bb and auto-detection
try "$GLOAT_BIN $EXAMPLE_DIR/factorial -t bb -r -- 3"
is "$rc" 0 "-r with -t bb and auto-detection works"
is "$got" "3! -> 6" "-r with -t bb and auto-detection produces correct output"

done-testing
