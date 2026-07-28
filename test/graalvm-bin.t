#!/usr/bin/env bash

# End-to-end GraalVM Native Image executable tests.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow GraalVM binary builds. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

cat > "$TMP/helper.clj" <<'EOF'
(ns graal.helper)

(defn greeting [name]
  (str "Hello, " name "!"))
EOF

cat > "$TMP/main.clj" <<'EOF'
(ns graal.app
  (:require [graal.helper :as helper]))

(defn -main [& args]
  (println (helper/greeting (or (first args) "World"))))
EOF

bin=$TMP/graal-app
try "gloat -q -Egraalvm -o $bin $TMP/main.clj $TMP/helper.clj 2>&1"
is "$rc" 0 "gloat -Egraalvm builds a multi-file Clojure binary"
ok "$([[ -x $bin ]])" "the GraalVM output is executable"

if [[ -x $bin ]]; then
  try "$bin Gloat"
  is "$rc" 0 "the GraalVM binary exits 0"
  is "$got" "Hello, Gloat!" "the GraalVM binary receives arguments"
fi

try "printf '%s\n' \
  '(ns main.core) (defn -main [& args] (println (or (first args) \"stdin\")))' |
  gloat -q -Egraalvm --run -- stdin-run 2>&1"
is "$rc" 0 "gloat -Egraalvm --run builds and runs stdin"
is "$got" "stdin-run" "GraalVM --run passes program arguments"

done-testing
