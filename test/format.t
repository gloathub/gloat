#!/usr/bin/env bash

# Tests for gloat Clojure formatting

source "$(dirname "${BASH_SOURCE[0]}")/init"

GLOAT_BIN=$PROJECT_ROOT/bin/gloat
SOURCE=$TMP/format.clj
OTHER=$TMP/format.txt
WIDE_SOURCE=$TMP/wide.clj
CLJFMT_SOURCE=$TMP/cljfmt.clj

printf '%s\n' '(defn greet[name](println "Hello,"name))' > "$SOURCE"
cp "$SOURCE" "$OTHER"
printf '%s\n' \
  '(defn wide-function [alpha beta gamma delta epsilon zeta eta theta] (+ alpha beta gamma delta epsilon zeta eta theta))' \
  > "$WIDE_SOURCE"
printf '(foo\nbar)\n' > "$CLJFMT_SOURCE"

LESS_BIN=$TMP/less-bin
mkdir -p "$LESS_BIN"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf "LESS_ARGS=<%s>\n" "$*" >&2' \
  'cat' \
  > "$LESS_BIN/less"
chmod +x "$LESS_BIN/less"

try "set -o pipefail; '$GLOAT_BIN' -F '$SOURCE' | cat"
is "$rc" 0 "'gloat -F file.clj' exits 0"
has "$got" "(defn greet [name]" "'gloat -F' formats the argument vector"
has "$got" '(println "Hello," name)' "'gloat -F' formats the function body"
hasnt "$got" $'\033[' "'gloat -F | cat' omits ANSI highlighting"
short_output=$got

try "set -o pipefail; '$GLOAT_BIN' --fmt '$SOURCE' | cat"
is "$rc" 0 "'gloat --fmt file.clj' exits 0"
is "$got" "$short_output" "'--fmt' matches '-F'"

try "set -o pipefail; GLOAT_FMT='cljfmt fix -' \
  '$GLOAT_BIN' -F '$CLJFMT_SOURCE' | cat"
is "$rc" 0 "'GLOAT_FMT=cljfmt gloat -F file.clj' exits 0"
is "$got" $'(foo\n bar)' "GLOAT_FMT selects cljfmt"
hasnt "$got" "Reformatted STDIN" \
  "Gloat suppresses cljfmt's routine success notice"

try "set -o pipefail; \
  GLOAT_FMT='cljfmt --function-arguments-indentation zprint fix -' \
  '$GLOAT_BIN' -F '$CLJFMT_SOURCE' | cat"
is "$rc" 0 "GLOAT_FMT accepts formatter-specific options"
is "$got" $'(foo\n  bar)' "cljfmt receives options from GLOAT_FMT"

printf '%s\n' '{:function-arguments-indentation :zprint}' > "$TMP/.cljfmt.edn"
try "set -o pipefail; cd '$TMP' && GLOAT_FMT='cljfmt fix -' \
  '$GLOAT_BIN' -F '$CLJFMT_SOURCE' | cat"
is "$rc" 0 "cljfmt formatting with a project config exits 0"
is "$got" $'(foo\n  bar)' "cljfmt discovers configuration from the cwd"

try "set -o pipefail; printf '%s\n' '(foo)' |
  GLOAT_FMT='tr a-z A-Z' '$GLOAT_BIN' -F | cat"
is "$rc" 0 "GLOAT_FMT accepts an arbitrary stdin-to-stdout command"
is "$got" "(FOO)" "Gloat runs the complete GLOAT_FMT command"

try "set -o pipefail; PATH='$LESS_BIN':\$PATH \
  '$GLOAT_BIN' -F '$SOURCE' | cat"
is "$rc" 0 "redirected formatter output exits 0 when less is available"
hasnt "$got" "LESS_ARGS=" "redirected formatter output does not invoke less"

if script --version 2>&1 | grep -q util-linux &&
  script -qec true /dev/null >/dev/null 2>&1
then
  for option in -F -C; do
    try "PATH='$LESS_BIN':\$PATH script -qec \
      \"'$GLOAT_BIN' $option '$SOURCE'\" /dev/null"
    is "$rc" 0 "'gloat $option' exits 0 on an interactive terminal"
    has "$got" "LESS_ARGS=<-rFRX>" \
      "'gloat $option' pages interactive output with less -rFRX"
  done

  for pager in none 0; do
    try "PATH='$LESS_BIN':\$PATH GLOAT_CLJ_PAGER=$pager script -qec \
      \"'$GLOAT_BIN' -F '$SOURCE'\" /dev/null"
    is "$rc" 0 "'GLOAT_CLJ_PAGER=$pager gloat -F' exits 0"
    hasnt "$got" "LESS_ARGS=" \
      "GLOAT_CLJ_PAGER=$pager disables paging on an interactive terminal"
  done

  try "PATH='$LESS_BIN':\$PATH GLOAT_CLJ_PAGER='less --custom' \
    script -qec \"'$GLOAT_BIN' -F '$SOURCE'\" /dev/null"
  is "$rc" 0 "a custom GLOAT_CLJ_PAGER command exits 0"
  has "$got" "LESS_ARGS=<--custom>" \
    "GLOAT_CLJ_PAGER accepts a complete pager command"
else
  pass "interactive less test requires an available util-linux pseudo-terminal"
fi

try "set -o pipefail; '$GLOAT_BIN' -F -w 40 '$WIDE_SOURCE' | cat"
is "$rc" 0 "'gloat -F -w 40 file.clj' exits 0"
has "$got" $'\n   eta theta]' "'-w 40' wraps the argument vector"
has "$got" $'\n  (+ alpha\n' "'-w 40' wraps the function body"
narrow_output=$got

try "set -o pipefail; '$GLOAT_BIN' --fmt --width=40 '$WIDE_SOURCE' | cat"
is "$rc" 0 "'gloat --fmt --width=40 file.clj' exits 0"
is "$got" "$narrow_output" "'--width=40' matches '-w 40'"

try "set -o pipefail; '$GLOAT_BIN' -Fw40 '$WIDE_SOURCE' | cat"
is "$rc" 0 "'gloat -Fw40 file.clj' exits 0"
is "$got" "$narrow_output" "'-Fw40' expands and sets formatting width"

try "set -o pipefail; '$GLOAT_BIN' -FCw40 '$WIDE_SOURCE' | cat"
is "$rc" 0 "'gloat -FCw40 file.clj' exits 0"
has "$got" $'\033[' "'gloat -FCw40' emits ANSI highlighting"
plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
is "$plain" "$narrow_output" "'-FCw40' formats to width before coloring"

try "$GLOAT_BIN -F --width=0 '$SOURCE'"
is "$rc" 1 "'gloat -F --width=0' exits 1"
has "$got" "--width must be a positive integer" \
  "'gloat -F --width=0' rejects zero"

try "$GLOAT_BIN -C -w 40 '$SOURCE'"
is "$rc" 1 "'gloat -C -w 40' exits 1"
has "$got" "--width requires --fmt" \
  "'gloat -C -w 40' requires formatting"

try "GLOAT_FMT='cljfmt fix -' '$GLOAT_BIN' -F -w 40 '$SOURCE'"
is "$rc" 1 "'GLOAT_FMT=cljfmt gloat -F -w 40' exits 1"
has "$got" "--width cannot be combined with GLOAT_FMT" \
  "GLOAT_FMT owns formatter-specific width options"

try "printf '%s\n' '(def widthless 1)' | '$GLOAT_BIN' -w40 -t clj"
is "$rc" 1 "'gloat -w40 -t clj' exits 1"
has "$got" "--width requires --fmt" \
  "'gloat -w40 -t clj' requires formatting"

try "$GLOAT_BIN --format '$SOURCE'"
is "$rc" 1 "'gloat --format' exits 1"
has "$got" "use '--fmt'" "'gloat --format' directs users to --fmt"

try "set -o pipefail; printf '%s\n' '(def answer(+ 40 2))' |
  '$GLOAT_BIN' --fmt - | cat"
is "$rc" 0 "'gloat --fmt -' exits 0"
has "$got" "(def answer (+ 40 2))" "'gloat --fmt -' formats stdin"
hasnt "$got" $'\033[' "'gloat --fmt - | cat' omits ANSI highlighting"

try "set -o pipefail; printf '%s\n' '(def implicit(+ 20 22))' |
  '$GLOAT_BIN' -F | cat"
is "$rc" 0 "'gloat -F' without a path exits 0"
is "$got" "(def implicit (+ 20 22))" \
  "'gloat -F' defaults to stdin"

try "$GLOAT_BIN -F '$SOURCE' '$SOURCE'"
is "$rc" 1 "'gloat -F' with multiple inputs exits 1"
has "$got" "accepts only one input path or -" \
  "'gloat -F' rejects multiple inputs"

try "$GLOAT_BIN -F '$TMP/missing.clj'"
is "$rc" 1 "'gloat -F missing.clj' exits 1"
has "$got" "input path is not readable" "'gloat -F' reports a missing input"

try "$GLOAT_BIN -F '$OTHER'"
is "$rc" 0 "'gloat -F file.txt' exits 0"
has "$got" "(defn greet [name]" "'gloat -F' accepts extensionless streams"

try "set -o pipefail; '$GLOAT_BIN' -F - \
  <(printf '%s\n' '(def from-stream(+ 20 22))') | cat"
is "$rc" 0 "'gloat -F - <(...)' exits 0"
has "$got" "(def from-stream (+ 20 22))" \
  "'gloat -F - <(...)' formats a process-substitution stream"

try "set -o pipefail; printf '%s\n' '(' | '$GLOAT_BIN' -F - | cat"
is "$rc" 1 "'gloat -F -' propagates zprint failure"
has "$got" "Unexpected EOF" "'gloat -F -' reports malformed Clojure input"

try "set -o pipefail; printf '%s\n' '(' |
  GLOAT_FMT='cljfmt fix -' '$GLOAT_BIN' -F - | cat"
is "$rc" 2 "'GLOAT_FMT=cljfmt gloat -F -' propagates cljfmt failure"
has "$got" "Failed to format file: STDIN" \
  "'GLOAT_FMT=cljfmt' preserves cljfmt diagnostics"

try "set -o pipefail; GLOAT_FMT='cljfmt fix -' \
  '$GLOAT_BIN' -FC '$SOURCE' | cat"
is "$rc" 0 "'GLOAT_FMT=cljfmt gloat -FC file.clj' exits 0"
has "$got" $'\033[' "'GLOAT_FMT=cljfmt gloat -FC' emits ANSI highlighting"
plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
is "$plain" '(defn greet [name] (println "Hello," name))' \
  "Gloat colors cljfmt output"

try "set -o pipefail; printf '%s\n' '(foo)' |
  GLOAT_FMT='definitely-not-a-formatter --flag' '$GLOAT_BIN' -F | cat"
is "$rc" 127 "a missing GLOAT_FMT command exits 127"
has "$got" "definitely-not-a-formatter: not found" \
  "the shell reports a missing GLOAT_FMT command"

try "set -o pipefail; '$GLOAT_BIN' -C '$SOURCE' | cat"
is "$rc" 0 "'gloat -C file.clj | cat' exits 0"
has "$got" $'\033[' "'gloat -C' always emits ANSI highlighting"
plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
is "$plain" '(defn greet[name](println "Hello,"name))' \
  "'gloat -C' does not format its input"

try "set -o pipefail; printf '%s\n' '(def implicit-color 42)' |
  '$GLOAT_BIN' -C | cat"
is "$rc" 0 "'gloat -C' without a path exits 0"
has "$got" $'\033[' "'gloat -C' defaults to stdin"

try "set -o pipefail; printf '%s\n' '(def colorized 42)' |
  '$GLOAT_BIN' --color - | cat"
is "$rc" 0 "'gloat --color - | cat' exits 0"
has "$got" $'\033[' "'gloat --color' highlights piped output"

for options in "-F -C" "-C -F" "-FC" "-CF" "--fmt --color"; do
  try "set -o pipefail; cd '$TMP' &&
    '$GLOAT_BIN' $options '$SOURCE' | cat"
  is "$rc" 0 "'gloat $options file.clj | cat' exits 0"
  has "$got" $'\033[' "'gloat $options' emits ANSI highlighting"
  plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
  is "$plain" "$short_output" \
    "'gloat $options' formats before syntax highlighting"
  ok "$([[ ! -e $TMP/format ]])" \
    "'gloat $options' does not fall through to compilation"
done

try "set -o pipefail; printf '%s\n' '(def combined(+ 20 22))' |
  '$GLOAT_BIN' -FC - | cat"
is "$rc" 0 "'gloat -FC -' exits 0"
has "$got" $'\033[' "'gloat -FC -' highlights formatted stdin"
plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
is "$plain" "(def combined (+ 20 22))" \
  "'gloat -FC -' formats stdin before highlighting"

try "set -o pipefail; printf '%s\n' '(def implicit-combined(+ 20 22))' |
  '$GLOAT_BIN' -FC | cat"
is "$rc" 0 "'gloat -FC' without a path exits 0"
has "$got" $'\033[' "'gloat -FC' highlights implicit stdin"
plain=$(printf '%s\n' "$got" | perl -pe 's/\e\[[0-9;]*m//g')
is "$plain" "(def implicit-combined (+ 20 22))" \
  "'gloat -FC' formats implicit stdin before highlighting"

try "$GLOAT_BIN -FCr '$SOURCE'"
is "$rc" 1 "'gloat -FCr' exits 1"
has "$got" "cannot be combined with '-r'" \
  "'gloat -FCr' applies normal compatibility validation"

try "$GLOAT_BIN -FCtgo '$SOURCE'"
is "$rc" 1 "'gloat -FCtgo' exits 1"
has "$got" "cannot be combined with '-t'" \
  "'-FCtgo' is normalized to '-F -C -t go'"

try "$GLOAT_BIN -FCrTfvqh '$SOURCE'"
is "$rc" 129 "a cluster containing every boolean short option is expanded"
has "$got" "usage:" "the expanded -h option displays help"

try "$GLOAT_BIN -qtclj '$SOURCE'"
is "$rc" 0 "'gloat -qtclj file.clj' exits 0"
has "$got" '(defn greet[name]' \
  "a clustered -t option accepts its attached value"

CLUSTER_OUTPUT=$TMP/cluster-output.clj
try "$GLOAT_BIN -qo'$CLUSTER_OUTPUT' '$SOURCE'"
is "$rc" 0 "'gloat -qoPATH file.clj' exits 0"
ok "$([[ -f $CLUSTER_OUTPUT ]])" \
  "a clustered -o option accepts its attached value"

try "$GLOAT_BIN -qEunknown '$SOURCE'"
is "$rc" 1 "'gloat -qEunknown file.clj' exits 1"
has "$got" "Unknown engine 'unknown'" \
  "a clustered -E option accepts its attached value"

try "$GLOAT_BIN -qXunknown '$SOURCE'"
is "$rc" 1 "'gloat -qXunknown file.clj' exits 1"
has "$got" "Unknown extension: unknown" \
  "a clustered -X option accepts its attached value"

try "$GLOAT_BIN --shell -- printf '%s\n' -FC"
is "$rc" 0 "'gloat --shell -- ... -FC' exits 0"
is "$got" "-FC" "short-option clusters after -- are preserved"

try "$GLOAT_BIN -h"
is "$rc" 129 "'gloat -h' exits 129"
has "$got" "-F, --fmt" "'gloat -h' lists the fmt option"
has "$got" "GLOAT_FMT" "'gloat -h' describes the formatter override"
has "$got" "-C, --color" "'gloat -h' lists the color option"
has "$got" "-w, --width" "'gloat -h' lists the width option"
has "$got" "--engines" "'gloat -h' lists the engines option"

for shell in bash zsh fish; do
  try "$GLOAT_BIN --complete=$shell"
  is "$rc" 0 "'gloat --complete=$shell' exits 0"
  if [[ $shell == fish ]]; then
    has "$got" "-l fmt" "$shell completion includes --fmt"
    has "$got" "-l color" "$shell completion includes --color"
    has "$got" "-l width" "$shell completion includes --width"
    has "$got" "-l engines" "$shell completion includes --engines"
    has "$got" "-l engine" "$shell completion includes --engine"
    has "$got" "-l time" "$shell completion includes --time"
    has "$got" "graalvm" "$shell completion includes graalvm"
    has "$got" "jolt" "$shell completion includes jolt"
  else
    has "$got" "--fmt" "$shell completion includes --fmt"
    has "$got" "--color" "$shell completion includes --color"
    has "$got" "--width" "$shell completion includes --width"
    has "$got" "--engines" "$shell completion includes --engines"
    has "$got" "--engine" "$shell completion includes --engine"
    has "$got" "--time" "$shell completion includes --time"
    has "$got" "graalvm" "$shell completion includes graalvm"
    has "$got" "jolt" "$shell completion includes jolt"
  fi
done

COMPLETION=$TMP/completion.bash
"$GLOAT_BIN" --complete=bash > "$COMPLETION"
try "source '$COMPLETION'
  COMP_WORDS=(gloat --fm)
  COMP_CWORD=1
  _gloat
  printf '%s\n' \"\${COMPREPLY[@]}\""
is "$rc" 0 "bash completion for '--fm' exits 0"
has "$got" "--fmt" "bash completion for '--fm' includes --fmt"

try "source '$COMPLETION'
  COMP_WORDS=(gloat --col)
  COMP_CWORD=1
  _gloat
  printf '%s\n' \"\${COMPREPLY[@]}\""
is "$rc" 0 "bash completion for '--col' exits 0"
has "$got" "--color" "bash completion for '--col' includes --color"

try "source '$COMPLETION'
  COMP_WORDS=(gloat --eng)
  COMP_CWORD=1
  _gloat
  printf '%s\n' \"\${COMPREPLY[@]}\""
is "$rc" 0 "bash completion for '--eng' exits 0"
has "$got" "--engine" "bash completion for '--eng' includes --engine"
has "$got" "--engines" "bash completion for '--eng' includes --engines"

done-testing
