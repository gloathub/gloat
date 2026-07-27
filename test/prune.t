#!/usr/bin/env bash

source "$(dirname "${BASH_SOURCE[0]}")/init"

loader=$PROJECT_ROOT/test/fixtures/prune-loader.go
direct_loader=$PROJECT_ROOT/test/fixtures/prune-direct-loader.go
linked_loader=$PROJECT_ROOT/test/fixtures/prune-linked-loader.go

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

try "bb -cp '$PROJECT_ROOT/src' -e '
  (require (quote prune))
  (let [parsed (prune/parse-loader (first *command-line-args*))
        context (prune/parse-ref-context parsed)
        block (second (first (:blocks parsed)))]
    (prn (prune/scan-block-refs block context)))
' '$direct_loader'"

is "$rc" 0 'prune scanner accepts cached external AOT calls'
has "$got" '["clojure.core" "println"]' \
  'prune scanner resolves cached external AOT calls to their Vars'

try "bb -cp '$PROJECT_ROOT/src' -e '
  (require (quote prune))
  (let [parsed (prune/parse-loader (first *command-line-args*))
        result (prune/generate-pruned-loader parsed #{\"keep\"})]
    (print (:output result)))
' '$direct_loader'"

is "$rc" 0 'prune generator accepts cached external AOT calls'
has "$got" 'aotExternalFn0 :=' \
  'prune keeps a cached external call used by a retained block'
hasnt "$got" 'aotExternalFn1 :=' \
  'prune removes a cached external call orphaned by block pruning'
has "$got" 'aotExternalFn2 :=' \
  'prune keeps a cached external call used by a captured-value initializer'
has "$got" 'var closed0 any' \
  'prune keeps a grouped captured-value declaration used by a retained block'
hasnt "$got" 'var closed1 any' \
  'prune removes an unused grouped captured-value declaration'
hasnt "$got" 'closed1 = "drop"' \
  'prune removes the unused captured-value initializer'

try "bb -cp '$PROJECT_ROOT/src' -e '
  (require (quote prune))
  (let [parsed (prune/parse-loader (first *command-line-args*))
        context (prune/parse-ref-context parsed)
        blocks (into {} (:blocks parsed))]
    (prn (prune/scan-block-refs (get blocks \"keep\") context))
    (prn (prune/scan-block-refs (get blocks \"helper\") context)))
' '$linked_loader'"

is "$rc" 0 'prune scanner accepts linked AOT calls'
has "$got" '["fixture" "helper"]' \
  'prune scanner resolves same-namespace direct AOT calls'
has "$got" '["clojure.core" "println"]' \
  'prune scanner resolves linked external AOT calls'

try "bb -cp '$PROJECT_ROOT/src' -e '
  (require (quote prune))
  (let [parsed (prune/parse-loader (first *command-line-args*))
        result (prune/generate-pruned-loader
                 parsed #{\"keep\" \"helper\"})]
    (print (:output result)))
' '$linked_loader'"

is "$rc" 0 'prune generator accepts linked AOT calls'
has "$got" 'aotDirectFn1 = ' \
  'prune keeps a directly called same-namespace function'
hasnt "$got" 'aotDirectFn2 = ' \
  'prune removes an orphaned same-namespace function'
has "$got" 'aotExternalFn0 :=' \
  'prune keeps a linked external call used by a retained block'
hasnt "$got" 'aotExternalFn1 :=' \
  'prune removes an orphaned linked external call'

try "gloat -qf -Xprune -o '$TMP/prune-main' \
  '$PROJECT_ROOT/test/fixtures/prune-main.clj'"
is "$rc" 0 'pruned binary builds with cached external AOT calls'

try "$TMP/prune-main"
is "$rc" 0 'pruned binary with cached external AOT calls runs'
is "$got" 'Hello from a pruned binary' \
  'pruned binary keeps the externally cached function'

try "gloat -qf -Xprune -o '$TMP/prune-multi' \
  '$PROJECT_ROOT/test/fixtures/multi-helper.clj' \
  '$PROJECT_ROOT/test/fixtures/multi-main.clj'"
is "$rc" 0 'pruned multi-file binary builds'

try "$TMP/prune-multi"
is "$rc" 0 'pruned multi-file binary runs'
is "$got" 'multi-file main' \
  'pruned multi-file binary initializes required user namespaces'

done-testing
