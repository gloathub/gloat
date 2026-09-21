#!/usr/bin/env bash

# A .glj input is Clojure that uses Glojure-only features such as Go
# interop. Gloat stages it unrewritten with its own extension and
# Glojure loads it, so the Go symbol below must resolve in the binary.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow .glj input binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/upper
cat > "$TMP/upper.glj" <<'GLJ'
(ns upper.core)
(defn -main [& args]
  (println (strings.ToUpper "glj input")))
GLJ

try "gloat -q -o $bin $TMP/upper.glj 2>&1"
is "$rc" 0 "gloat -o builds a binary from .glj input"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

try "$bin"
is "$got" "GLJ INPUT" "Go interop in the .glj source runs in the binary"

try "gloat -q -t clj $TMP/upper.glj"
is "$rc" 0 "gloat -t clj accepts .glj input"
has "$got" "strings.ToUpper" "gloat -t clj prints the .glj source unrewritten"

done-testing
