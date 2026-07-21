#!/usr/bin/env bash

source "$(dirname "${BASH_SOURCE[0]}")/init"

loader=$PROJECT_ROOT/test/fixtures/prune-loader.go

try "bb -cp '$PROJECT_ROOT/src' -e '
  (require (quote prune))
  (let [parsed (prune/parse-loader (first *command-line-args*))
        result (prune/generate-pruned-loader parsed #{\"keep\"})]
    (print (:output result)))
' '$loader'"

is "$rc" 0 'prune parser accepts checked and unchecked symbol constructors'
has "$got" 'lang.NewSymbol("keep")' 'prune keeps a used checked symbol'
hasnt "$got" 'lang.NewSymbolUnchecked("drop")' \
  'prune removes an unused unchecked symbol'

done-testing
