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
let_go_version=$(awk '$1 == "LET-GO-VERSION" {print $3; exit}' \
  "$PROJECT_ROOT/common/common.mk")
if [[ -x $PROJECT_ROOT/.cache/local/let-go-$let_go_version/bin/lg ]]; then
  has "$got" "lg    v$let_go_version" \
    "'gloat --version' shows installed lg version"
else
  hasnt "$got" "lg    v" \
    "'gloat --version' omits uninstalled lg version"
fi

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
try "$GLOAT_BIN --engines"
is "$rc" 0 "'gloat --engines' exits 0"
has "$got" "Available compilation engines" \
  "'gloat --engines' prints its heading"
has "$got" "glj     Glojure (default)" \
  "'gloat --engines' lists glj"
has "$got" "graalvm GraalVM Native Image (binaries only)" \
  "'gloat --engines' lists graalvm"
has "$got" "jolt    Jolt (binaries only)" \
  "'gloat --engines' lists jolt"
has "$got" "lgvm    let-go bytecode VM" \
  "'gloat --engines' lists lgvm"
has "$got" "lglvm   let-go native lowering with VM fallback" \
  "'gloat --engines' lists lglvm"
has "$got" "lgl     let-go native lowering (not yet implemented)" \
  "'gloat --engines' describes lgl status"

try "$GLOAT_BIN -Efoo x.clj"
is "$rc" 1 "'gloat -Efoo' exits 1"
has "$got" "Unknown engine 'foo'" "'gloat -Efoo' reports unknown engine"
has "$got" "glojure (glj), graalvm, jolt, let-go-lower (lgl), let-go-lower-vm (lglvm), let-go-vm (lgvm)" "'gloat -Efoo' lists known engines"

try "GLOAT_ENGINE=foo $GLOAT_BIN -t clj x.clj"
is "$rc" 1 "'GLOAT_ENGINE=foo gloat' exits 1"
has "$got" "Unknown engine 'foo'" "'GLOAT_ENGINE=foo' reports unknown engine"

try "$GLOAT_BIN -Elgvm -t wasm x.clj"
is "$rc" 1 "'gloat -Elgvm -t wasm' exits 1"
has "$got" "Engine 'let-go-vm' does not yet support format 'wasm'" \
  "'gloat -Elgvm' reports let-go-vm unsupported formats"

try "$GLOAT_BIN --engine=let-go-vm -t go x.clj"
is "$rc" 1 "'gloat --engine=let-go-vm -t go' exits 1"
has "$got" "Engine 'let-go-vm' does not yet support format 'go'" \
  "'gloat --engine=let-go-vm' reports format in error"

try "GLOAT_ENGINE=lgvm $GLOAT_BIN -o x.so x.clj"
is "$rc" 1 "'GLOAT_ENGINE=lgvm gloat -o x.so' validates input"
has "$got" "Input file/directory does not exist: x.clj" \
  "'GLOAT_ENGINE=lgvm' accepts lib inferred from -o"

# GraalVM engine validation (binary-only, self-contained Clojure)
try "$GLOAT_BIN -Egraalvm -t lib x.clj"
is "$rc" 1 "'gloat -Egraalvm -t lib' exits 1"
has "$got" "does not support format 'lib' (supported: bin)" \
  "'gloat -Egraalvm' reports its supported format"

try "$GLOAT_BIN -Egraalvm --platform=linux/amd64 x.clj"
is "$rc" 1 "'gloat -Egraalvm --platform' exits 1"
has "$got" "does not support --platform" \
  "'gloat -Egraalvm' rejects cross-compilation"

try "$GLOAT_BIN -Egraalvm --module=example.com/app x.clj"
is "$rc" 1 "'gloat -Egraalvm --module' exits 1"
has "$got" "does not support --module" \
  "'gloat -Egraalvm' rejects Go module configuration"

try "$GLOAT_BIN -Egraalvm -Xprune x.clj"
is "$rc" 1 "'gloat -Egraalvm -Xprune' exits 1"
has "$got" "does not support -X/--ext" \
  "'gloat -Egraalvm' rejects Go processing extensions"

printf '{:deps {}}\n' > "$TMP/gljdeps.edn"
try "$GLOAT_BIN -Egraalvm --deps=$TMP/gljdeps.edn x.clj"
is "$rc" 1 "'gloat -Egraalvm --deps' exits 1"
has "$got" "does not support gljdeps.edn" \
  "'gloat -Egraalvm' rejects Go dependency configuration"

try "$GLOAT_BIN -Egraalvm -o $TMP/graal-ys $FIXTURES_DIR/hello.ys"
is "$rc" 1 "'gloat -Egraalvm hello.ys' exits 1"
has "$got" "only supports Clojure (.clj) input" \
  "'gloat -Egraalvm' rejects YAMLScript input"

printf '(defn -main [] (println \"missing namespace\"))\n' \
  > "$TMP/graal-no-ns.clj"
try "$GLOAT_BIN -Egraalvm -o $TMP/graal-no-ns $TMP/graal-no-ns.clj"
is "$rc" 1 "'gloat -Egraalvm' rejects namespace-free files"
has "$got" "requires every source file to declare an (ns ...) form" \
  "'gloat -Egraalvm' explains its namespace requirement"

# Jolt engine validation (binary-only, project-aware Clojure)
try "$GLOAT_BIN -Ejolt -t lib x.clj"
is "$rc" 1 "'gloat -Ejolt -t lib' exits 1"
has "$got" "does not support format 'lib' (supported: bin)" \
  "'gloat -Ejolt' reports its supported format"

try "$GLOAT_BIN -Ejolt --platform=linux/amd64 x.clj"
is "$rc" 1 "'gloat -Ejolt --platform' exits 1"
has "$got" "does not support --platform" \
  "'gloat -Ejolt' rejects cross-compilation"

try "$GLOAT_BIN -Ejolt --module=example.com/app x.clj"
is "$rc" 1 "'gloat -Ejolt --module' exits 1"
has "$got" "does not support --module" \
  "'gloat -Ejolt' rejects Go module configuration"

try "$GLOAT_BIN -Ejolt -Xgzip x.clj"
is "$rc" 1 "'gloat -Ejolt -Xgzip' exits 1"
has "$got" "only supports -Xprune" \
  "'gloat -Ejolt' rejects non-prune extensions"

try "$GLOAT_BIN -Ejolt --deps=$TMP/gljdeps.edn x.clj"
is "$rc" 1 "'gloat -Ejolt --deps' exits 1"
has "$got" "use deps.edn for Jolt dependencies" \
  "'gloat -Ejolt' rejects Glojure dependency configuration"

printf '(ns unsupported)\n' > "$TMP/jolt.glj"
try "$GLOAT_BIN -Ejolt -o $TMP/jolt-glj $TMP/jolt.glj"
is "$rc" 1 "'gloat -Ejolt input.glj' exits 1"
has "$got" "only supports Clojure (.clj) and YAMLScript (.ys) input" \
  "'gloat -Ejolt' rejects Glojure input"

printf '(defn -main [] (println "missing namespace"))\n' \
  > "$TMP/jolt-no-ns.clj"
try "$GLOAT_BIN -Ejolt -o $TMP/jolt-no-ns $TMP/jolt-no-ns.clj"
is "$rc" 1 "'gloat -Ejolt' rejects namespace-free files"
has "$got" "requires every source file to declare an (ns ...) form" \
  "'gloat -Ejolt' explains its namespace requirement"

printf '%s\n' '(ns jolt.no-main)' '(def answer 42)' \
  > "$TMP/jolt-no-main.clj"
try "$GLOAT_BIN -Ejolt -o $TMP/jolt-no-main $TMP/jolt-no-main.clj"
is "$rc" 1 "'gloat -Ejolt' rejects a source without -main"
has "$got" "requires a (defn -main ...) entry point" \
  "'gloat -Ejolt' explains its entry-point requirement"

JOLT_MOCK=$TMP/jolt-mock
JOLT_LOG=$TMP/jolt.log
cat > "$JOLT_MOCK" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'JOLT_PWD=%s\n' "${JOLT_PWD-}" > "$JOLT_LOG"
printf 'JOLT_CACHE_DIR=%s\n' "${JOLT_CACHE_DIR-}" >> "$JOLT_LOG"
printf 'JOLT_RUNTIME_CACHE_DIR=%s\n' "${JOLT_RUNTIME_CACHE_DIR-}" >> "$JOLT_LOG"
printf 'ARG=%s\n' "$@" >> "$JOLT_LOG"
out=
while (($#)); do
  if [[ $1 == -o ]]; then
    out=$2
    break
  fi
  shift
done
[[ $out ]]
printf '#!/usr/bin/env bash\nprintf "mock jolt\\n"\n' > "$out"
chmod +x "$out"
EOF
chmod +x "$JOLT_MOCK"
export JOLT_LOG

YS_MOCK=$TMP/ys-mock
cat > "$YS_MOCK" <<'EOF'
#!/usr/bin/env bash
cat <<'STAR'
(require '[clojurestar.deps :as deps])
(deps/add-deps '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.31"}}})
(ns main (:require ys.v0))
(ys.v0/init)
(defn main [] (say "Hello"))
(apply main ARGS)
STAR
EOF
chmod +x "$YS_MOCK"

try "GLOAT_YS=$YS_MOCK GLOAT_JOLT=$JOLT_MOCK $GLOAT_BIN -q -Ejolt \
  -o $TMP/jolt-ys $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Ejolt' accepts YAMLScript input"
has "$(< "$JOLT_LOG")" "ARG=hello.core" \
  "'gloat -Ejolt' stages YAMLScript as portable Clojure"

YS_LIB_MOCK=$TMP/ys-lib-mock
cat > "$YS_LIB_MOCK" <<'EOF'
#!/usr/bin/env bash
cat <<'STAR'
(require '[clojurestar.deps :as deps])
(deps/add-deps '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.32"}}})
(ns main (:require ys.v0))
(ys.v0/init)
(def EXPORT {"twice" ["int" "int"]})
(defn twice [x] (* x 2))
STAR
EOF
chmod +x "$YS_LIB_MOCK"
printf '%s\n' '!ys-0' > "$TMP/export.ys"

try "GLOAT_YS=$YS_LIB_MOCK $GLOAT_BIN -q -t clj $TMP/export.ys"
is "$rc" 0 "portable EXPORT source converts to Clojure"
has "$got" "(def EXPORT" "portable EXPORT declaration is retained"
hasnt "$got" "(defn -main" "portable EXPORT source gets no main wrapper"

JOLT_PROJECT=$TMP/jolt-project
mkdir -p "$JOLT_PROJECT/src/multi"
cp "$FIXTURES_DIR/multi-main.clj" "$JOLT_PROJECT/src/multi/app.clj"
cp "$FIXTURES_DIR/multi-helper.clj" "$JOLT_PROJECT/src/multi/helper.clj"
printf '{:paths ["src"]}\n' > "$JOLT_PROJECT/deps.edn"

cat > "$JOLT_PROJECT/src/multi/other.clj" <<'EOF'
(ns multi.other)

(defn -main [& _]
  (println "other"))
EOF

try "GLOAT_JOLT=$JOLT_MOCK $GLOAT_BIN -q -Ejolt -o $TMP/jolt-many $JOLT_PROJECT"
is "$rc" 1 "'gloat -Ejolt' rejects ambiguous -main namespaces"
has "$got" "received multiple -main namespaces" \
  "'gloat -Ejolt' asks the user to select an entry namespace"

try "GLOAT_JOLT=$JOLT_MOCK $GLOAT_BIN -q -Ejolt --ns=multi.app -Xprune -o $TMP/jolt-app $JOLT_PROJECT"
is "$rc" 0 "'gloat -Ejolt' accepts a deps.edn project directory"
ok "$([[ -x $TMP/jolt-app ]])" \
  "'gloat -Ejolt' preserves executable output permissions"
has "$(< "$JOLT_LOG")" "JOLT_PWD=$JOLT_PROJECT" \
  "'gloat -Ejolt' uses the input directory as Jolt's project root"
has "$(< "$JOLT_LOG")" \
  "JOLT_CACHE_DIR=$PROJECT_ROOT/.cache/local/home/jolt/aot-cache" \
  "'gloat -Ejolt' keeps Jolt's compiler cache under Gloat's cache"
has "$(< "$JOLT_LOG")" "ARG=-A:gloat/jolt-engine" \
  "'gloat -Ejolt' adds its staged source alias"
has "$(< "$JOLT_LOG")" "ARG=multi.app" \
  "'gloat -Ejolt' selects the -main namespace"
has "$(< "$JOLT_LOG")" "ARG=--tree-shake" \
  "'gloat -Ejolt -Xprune' enables Jolt tree shaking"

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
try "$GLOAT_BIN -Elgvm --run $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elgvm --run' exits 0"
is "$got" "Hello, World!" "'gloat -Elgvm --run' runs the program"

try "$GLOAT_BIN -Elgvm --run $FIXTURES_DIR/hello.ys -- Gloat"
is "$got" "Hello, Gloat!" "'gloat -Elgvm --run' passes program args"

printf '(defn -main [] (throw (ex-info "boom" {})))' > "$TMP/boom.clj"
try "$GLOAT_BIN -Elgvm --run $TMP/boom.clj"
is "$rc" 1 "'gloat -Elgvm --run' propagates failure exit code"

# --time prints the run time (not compile) to stderr
try "$GLOAT_BIN -Elgvm --run --time $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elgvm --run --time' exits 0"
has "$got" "Hello, World!" "'--time' run still prints program output"
like "$got" "> gloat run time: [0-9]\.[0-9][0-9][0-9]s" \
  "'--time' prints labeled run time"

try "$GLOAT_BIN --time -o x.lg $FIXTURES_DIR/hello.ys"
is "$rc" 1 "'gloat --time' without --run exits 1"
has "$got" "requires --run" "'--time' requires --run"

# bin format under the lg engine (bundled binary via lg -b)
try "$GLOAT_BIN -Elgvm -o $TMP/hello-lg $FIXTURES_DIR/hello.ys"
is "$rc" 0 "'gloat -Elgvm -o bin' exits 0"

try "$TMP/hello-lg"
is "$got" "Hello, World!" "lg-bundled binary runs standalone"

try "$TMP/hello-lg Gloat"
is "$got" "Hello, Gloat!" "lg-bundled binary takes args"

# LG engine (native Go via let-go AOT lowering)
try "$GLOAT_BIN -Elglvm -t wasm x.clj"
is "$rc" 1 "'gloat -Elglvm -t wasm' exits 1"
has "$got" "Engine 'let-go-lower-vm' does not yet support format 'wasm'" \
  "'gloat -Elglvm' reports let-go-lower-vm unsupported formats"
has "$got" "(supported: LG, bin, lib)" \
  "'gloat -Elglvm' lists let-go-lower-vm supported formats"

try "GLOAT_ENGINE=let-go-lower-vm $GLOAT_BIN -o x.so x.clj"
is "$rc" 1 "'GLOAT_ENGINE=let-go-lower-vm gloat -o x.so' validates input"
has "$got" "Input file/directory does not exist: x.clj" \
  "'GLOAT_ENGINE=let-go-lower-vm' accepts lib inferred from -o"

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

try "env MAKEFLAGS='YAMLSCRIPT-VERSION=999.999.999' \
  '$GLOAT_BIN' -q boolean.clj -t clj"
is "$rc" 0 "Clojure input does not install the YAMLScript compiler"
has "$got" "(ns boolean" "Clojure input compiles without a ys executable"

try "$GLOAT_BIN hello.ys -t clj"
is "$rc" 0 "'gloat hello.ys -t clj' exits 0"
has "$got" "(ns " "'gloat hello.ys -t clj' outputs Clojure"

if [[ ${RUN_SLOW_TESTS:-} ]]; then
  # Test Go stdout mode
  try "$GLOAT_BIN hello.ys -t go"
  is "$rc" 0 "'gloat hello.ys -t go' exits 0"
  has "$got" "// Code generated by glojure" "'gloat hello.ys -t go' outputs Go code"

  star_fixture=$FIXTURES_DIR/star.ys
  star_m2=$TMP/star-m2
  star_gitlibs=$TMP/star-gitlibs
  try "YS_MAVEN_REPOSITORY='$star_m2' \
    YS_GITLIBS_DIR='$star_gitlibs' \
    '$GLOAT_BIN' -q -o '$TMP/star-bin' '$star_fixture'"
  is "$rc" 0 "portable use dependency builds a native binary"

  try "YS_MAVEN_REPOSITORY='$star_m2' \
    YS_GITLIBS_DIR='$star_gitlibs' '$TMP/star-bin'"
  is "$rc" 0 "portable use dependency loads at runtime"
  is "$got" $'{1 [1 2], 2 [2 1]}\n42' \
    "portable use binary prints dependency-backed output"

  try "YS_MAVEN_REPOSITORY='$star_m2' \
    YS_GITLIBS_DIR='$star_gitlibs' \
    '$GLOAT_BIN' -q <(ys --to=star '$star_fixture') \
    -o '$TMP/star-stream-bin'"
  is "$rc" 0 "portable star stream builds a native binary"

  try "YS_MAVEN_REPOSITORY='$star_m2' \
    YS_GITLIBS_DIR='$star_gitlibs' '$TMP/star-stream-bin'"
  is "$rc" 0 "portable star stream loads dependencies at runtime"
  is "$got" $'{1 [1 2], 2 [2 1]}\n42' \
    "portable star stream prints dependency-backed output"

  printf '%s\n' '!ys-0' 'say: 40 + 2' > "$TMP/star-script.ys"
  try "'$GLOAT_BIN' -q <(ys --to=star '$TMP/star-script.ys') \
    -o '$TMP/star-script-bin'"
  is "$rc" 0 "top-level portable star stream builds a native binary"

  try "'$TMP/star-script-bin'"
  is "$rc" 0 "top-level portable star stream runs"
  is "$got" 42 "top-level portable star stream prints its result"

  try "YS_MAVEN_REPOSITORY='$star_m2' \
    YS_GITLIBS_DIR='$star_gitlibs' \
    '$GLOAT_BIN' -q -Xprune -o '$TMP/star-pruned' '$star_fixture'"
  is "$rc" 1 "portable use with -Xprune exits 1"
  has "$got" "Portable use forms are not compatible with -Xprune" \
    "portable use with -Xprune reports the unsupported combination"
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

# A multi-file binary uses the namespace that defines -main as its entrypoint,
# regardless of the explicit input order.
try "$GLOAT_BIN --force '$FIXTURES_DIR/multi-helper.clj' '$FIXTURES_DIR/multi-main.clj' -o '$TMP/multi-bin'"
is "$rc" 0 "'gloat helper main -o bin' exits 0"
try "$TMP/multi-bin"
is "$rc" 0 "multi-file binary runs"
is "$got" "multi-file main" "multi-file binary selects the -main namespace"

done-testing
