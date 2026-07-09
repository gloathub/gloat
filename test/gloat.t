#!/usr/bin/env bash

# Tests for gloat CLI

source "$(dirname "${BASH_SOURCE[0]}")/init"

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
GLOAT_BIN=$SCRIPT_DIR/../bin/gloat
FIXTURES_DIR=$SCRIPT_DIR/fixtures

# Test help/version (exit 129 is normal for git-based help display)
try "$GLOAT_BIN"
is "$rc" 129 "'gloat' with no args exits 129"
has "$got" "usage:" "'gloat' with no args shows usage"

try "$GLOAT_BIN -h"
is "$rc" 129 "'gloat -h' exits 129"
has "$got" "usage:" "'gloat -h' shows usage"

try "$GLOAT_BIN --version"
is "$rc" 0 "'gloat --version' exits 0"
has "$got" "gloat v" "'gloat --version' shows gloat version"
has "$got" "glj   v" "'gloat --version' shows glj version"

try "$GLOAT_BIN --which=glj"
is "$rc" 0 "'gloat --which=glj' exits 0"
has "$got" "/glj" "'gloat --which=glj' prints glj path"

try "$GLOAT_BIN --which=glj,go"
is "$rc" 0 "'gloat --which=glj,go' exits 0"
has "$got" "/glj" "'gloat --which=glj,go' prints glj path"
has "$got" "/go" "'gloat --which=glj,go' prints go path"

try "$GLOAT_BIN --which=wasmtime"
is "$rc" 0 "'gloat --which=wasmtime' exits 0"
has "$got" "/wasmtime" "'gloat --which=wasmtime' prints managed wasmtime path"

try "$GLOAT_BIN --which=lg"
is "$rc" 0 "'gloat --which=lg' exits 0"
has "$got" "/lg" "'gloat --which=lg' prints managed lg path"

try "$GLOAT_BIN --which=definitely-not-a-gloat-command"
is "$rc" 1 "'gloat --which=missing' exits 1"
is "$got" "" "'gloat --which=missing' is quiet"

try "$GLOAT_BIN --which=ls,wasmtime,definitely-not-a-gloat-command,bash"
is "$rc" 1 "'gloat --which' exits 1 if any command is missing"
has "$got" "/ls" "'gloat --which' prints paths before missing command"
has "$got" "/wasmtime" "'gloat --which' prints managed paths"
has "$got" "/bash" "'gloat --which' continues after missing command"

try "$GLOAT_BIN --which="
is "$rc" 1 "'gloat --which=' exits 1"
has "$got" "requires a command name" "'gloat --which=' reports empty command"

try "$GLOAT_BIN --which=glj,"
is "$rc" 1 "'gloat --which=glj,' exits 1"
has "$got" "empty command name" "'gloat --which=glj,' reports empty command"

try "$GLOAT_BIN --glj"
is "$rc" 129 "'gloat --glj' is no longer accepted"

# Engine selection (-E/--engine/GLOAT_ENGINE)
try "$GLOAT_BIN -Efoo x.clj"
is "$rc" 1 "'gloat -Efoo' exits 1"
has "$got" "Unknown engine 'foo'" "'gloat -Efoo' reports unknown engine"
has "$got" "LG, glj, lg" "'gloat -Efoo' lists known engines"

try "GLOAT_ENGINE=foo $GLOAT_BIN -t clj x.clj"
is "$rc" 1 "'GLOAT_ENGINE=foo gloat' exits 1"
has "$got" "Unknown engine 'foo'" "'GLOAT_ENGINE=foo' reports unknown engine"

try "$GLOAT_BIN -Elg -t wasm x.clj"
is "$rc" 1 "'gloat -Elg -t wasm' exits 1"
has "$got" "Engine 'lg' does not yet support format 'wasm'" \
  "'gloat -Elg' reports lg engine unsupported formats"

try "$GLOAT_BIN --engine=lg -t go x.clj"
is "$rc" 1 "'gloat --engine=lg -t go' exits 1"
has "$got" "Engine 'lg' does not yet support format 'go'" \
  "'gloat --engine=lg' reports format in error"

try "GLOAT_ENGINE=lg $GLOAT_BIN -o x.so x.clj"
is "$rc" 1 "'GLOAT_ENGINE=lg gloat -o x.so' exits 1"
has "$got" "Engine 'lg' does not yet support format 'lib'" \
  "'GLOAT_ENGINE=lg' infers format from -o"

# lg output format (-t lg implies the lg engine)
lg_bin=$("$GLOAT_BIN" --which=lg | tail -1)
lg_paths=$SCRIPT_DIR/../ys/lg:.

try "$GLOAT_BIN -q -t lg $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -t lg' exits 0"
has "$got" "[ys.v0 :refer :all]" "'gloat -t lg' output requires ys.v0"
has "$got" "(ns main.core" "'gloat -t lg' output contains main namespace"
hasnt "$got" "(ns ys.v0" "'gloat -t lg' does not inline the ys runtime"

try "$GLOAT_BIN -q -t lg -o $TMP/hello.lg $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -t lg -o hello.lg' exits 0"

try "$lg_bin -source-paths $lg_paths $TMP/hello.lg"
is "$rc" 0 "lg runs the generated hello.lg"
is "$got" "Hello, World!" "hello.lg prints default greeting"

try "$lg_bin -source-paths $lg_paths $TMP/hello.lg Gloat"
is "$got" "Hello, Gloat!" "hello.lg greets command-line argument"

# --run under the lg engine
try "$GLOAT_BIN -Elg --run $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elg --run' exits 0"
is "$got" "Hello, World!" "'gloat -Elg --run' runs the program"

try "$GLOAT_BIN -Elg --run $FIXTURES_DIR/hello.ys -- Gloat"
is "$got" "Hello, Gloat!" "'gloat -Elg --run' passes program args"

printf '(defn -main [] (throw (ex-info "boom" {})))' > "$TMP/boom.clj"
try "$GLOAT_BIN -Elg --run $TMP/boom.clj"
is "$rc" 1 "'gloat -Elg --run' propagates failure exit code"

# --time prints the run time (not compile) to stderr
try "$GLOAT_BIN -Elg --run --time $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elg --run --time' exits 0"
has "$got" "Hello, World!" "'--time' run still prints program output"
like "$got" "> gloat run time: [0-9]\.[0-9][0-9][0-9]s" \
  "'--time' prints labeled run time"

try "$GLOAT_BIN --time -o x.lg $FIXTURES_DIR/hello.ys"
is "$rc" 1 "'gloat --time' without --run exits 1"
has "$got" "requires --run" "'--time' requires --run"

# bin format under the lg engine (bundled binary via lg -b)
try "$GLOAT_BIN -Elg -o $TMP/hello-lg $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elg -o bin' exits 0"

try "$TMP/hello-lg"
is "$got" "Hello, World!" "lg-bundled binary runs standalone"

try "$TMP/hello-lg Gloat"
is "$got" "Hello, Gloat!" "lg-bundled binary takes args"

# LG engine (native Go via let-go AOT lowering)
try "$GLOAT_BIN -ELG -t wasm x.clj"
is "$rc" 1 "'gloat -ELG -t wasm' exits 1"
has "$got" "Engine 'LG' does not yet support format 'wasm'" \
  "'gloat -ELG' reports LG engine unsupported formats"
has "$got" "(supported: LG, bin)" \
  "'gloat -ELG' lists LG engine supported formats"

try "GLOAT_ENGINE=LG $GLOAT_BIN -o x.so x.clj"
is "$rc" 1 "'GLOAT_ENGINE=LG gloat -o x.so' exits 1"
has "$got" "Engine 'LG' does not yet support format 'lib'" \
  "'GLOAT_ENGINE=LG' infers format from -o"

# -t LG emits the lowered Go source of the program namespace
try "$GLOAT_BIN -q -t LG $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -t LG' exits 0"
has "$got" "package core" "'gloat -t LG' emits a Go package"
has "$got" "vm.ExecContext" "'gloat -t LG' emits let-go native fns"
# -main lowers variadic; its Go name is Main__main when the program's
# own main fn (multi-arity, also lowered) claims the bare Main name
has "$got" "func Main__main(" "'gloat -t LG' lowers the -main entry"

try "$GLOAT_BIN --shell -- 'printf \"%s\\n\" \"\${PATH%%:*}\"' 2>/dev/null"
is "$rc" 0 "'gloat --shell' exits 0"
ok "$([[ $got == "$PROJECT_ROOT/.cache/"* ||
        ( ${GLOJURE_DIR:-} && $got == "$GLOJURE_DIR"/bin/* ) ]])" \
  "'gloat --shell' promotes gloat cache paths"

try "$GLOAT_BIN --repl <<<'(println \"hello\")'"
is "$rc" 0 "'gloat --repl' exits 0 with piped input"
is "$got" "hello" "'gloat --repl' suppresses banner with piped input"

try "$GLOAT_BIN --repl --quiet <<<'(println \"hello\")'"
is "$rc" 0 "'gloat --repl --quiet' exits 0 with piped input"
is "$got" "hello" "'gloat --repl --quiet' suppresses banner with piped input"

if cd "$TMP" &&
   "$GLOAT_BIN" --classpath=. <<<'(load "__missing__")' 2>&1 |
     grep -q 'failed to load __missing__:'
then
  printf '%s\n' '(println "hello")' > "$TMP/hello.clj"
  try "cd '$TMP' && '$GLOAT_BIN' --classpath=. <<<'(load \"hello\")'"
  is "$rc" 0 "'gloat --classpath=.' exits 0 with bare load"
  is "$got" "hello" "'gloat --classpath=.' loads resource from classpath"

  try "cd '$TMP' && '$GLOAT_BIN' --classpath=. <<<'(load \"/hello\")'"
  is "$rc" 0 "'gloat --classpath=.' exits 0 with absolute resource load"
  is "$got" "hello" "'gloat --classpath=.' loads absolute resource from classpath"

  try "cd '$TMP' && '$GLOAT_BIN' --classpath=. <<<'(load \"hello.clj\")'"
  is "$rc" 1 "'gloat --classpath=.' rejects filename-style load"
  has "$got" "failed to load hello.clj" \
    "'gloat --classpath=.' treats dots as resource separators"

  try "cd '$TMP' && '$GLOAT_BIN' --classpath=. <<<'(load \"/hello.clj\")'"
  is "$rc" 1 "'gloat --classpath=.' rejects absolute filename-style load"
  has "$got" "failed to load /hello.clj" \
    "'gloat --classpath=.' treats absolute dots as resource separators"
fi

# Test stdout modes
cd "$FIXTURES_DIR" || bail-out "Cannot cd to fixtures"

try "$GLOAT_BIN hello.ys -t clj"
is "$rc" 0 "'gloat hello.ys -t clj' exits 0"
has "$got" "(ns " "'gloat hello.ys -t clj' outputs Clojure"

if [[ ${RUN_SLOW_TESTS:-} ]]; then
  # Test Go stdout mode
  try "$GLOAT_BIN hello.ys -t go"
  is "$rc" 0 "'gloat hello.ys -t go' exits 0"
  has "$got" "// Code generated by glojure" "'gloat hello.ys -t go' outputs Go code"
fi

# Test error handling
try "$GLOAT_BIN nonexistent.ys"
is "$rc" 1 "'gloat nonexistent.ys' exits 1"
has "$got" "does not exist" "'gloat nonexistent.ys' shows error"

# Test -t .ext shorthand
rm -f "$FIXTURES_DIR/hello.glj"
try "$GLOAT_BIN hello.ys -t .glj"
is "$rc" 0 "'gloat hello.ys -t .glj' exits 0"
ok "$([[ -f $FIXTURES_DIR/hello.glj ]])" "'gloat hello.ys -t .glj' creates hello.glj"
rm -f "$FIXTURES_DIR/hello.glj"

if [[ ${RUN_SLOW_TESTS:-} ]]; then
  # Test -t .go shorthand
  rm -f "$FIXTURES_DIR/hello.go"
  try "$GLOAT_BIN hello.ys -t .go"
  is "$rc" 0 "'gloat hello.ys -t .go' exits 0"
  ok "$([[ -f $FIXTURES_DIR/hello.go ]])" "'gloat hello.ys -t .go' creates hello.go"
  rm -f "$FIXTURES_DIR/hello.go"
fi

# Test fail-fast when output exists (file)
touch "$FIXTURES_DIR/exists-file"
try "$GLOAT_BIN hello.ys -o $FIXTURES_DIR/exists-file"
is "$rc" 1 "'gloat' fails when output file exists"
has "$got" "Output already exists" "'gloat' shows error for existing file"
rm -f "$FIXTURES_DIR/exists-file"

# Test fail-fast when output exists (directory where file expected)
mkdir -p "$FIXTURES_DIR/exists-dir"
try "$GLOAT_BIN hello.ys -o $FIXTURES_DIR/exists-dir"
is "$rc" 1 "'gloat' fails when output dir exists where file expected"
has "$got" "Output already exists" "'gloat' shows error for existing dir"
rmdir "$FIXTURES_DIR/exists-dir"

# Test fail-fast when file exists where directory expected
touch "$FIXTURES_DIR/exists-as-file"
try "$GLOAT_BIN hello.ys -o $FIXTURES_DIR/exists-as-file/"
is "$rc" 1 "'gloat' fails when file exists where dir expected"
has "$got" "Output already exists" "'gloat' shows error for file blocking dir"
rm -f "$FIXTURES_DIR/exists-as-file"

# Test multiple input files rejection
try "$GLOAT_BIN hello.ys hello.ys"
is "$rc" 1 "'gloat f1 f2' exits 1"
has "$got" "Multiple input files require -o output" \
  "'gloat f1 f2' shows multiple files error"

done-testing
