#!/usr/bin/env bash

# End-to-end Jolt native executable tests.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Jolt binary builds. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

project=$TMP/jolt-project
helper=$TMP/jolt-helper
mkdir -p "$project/src/jolt_app" "$helper/src/jolt_helper"

cat > "$helper/deps.edn" <<'EOF'
{:paths ["src"]}
EOF

cat > "$helper/src/jolt_helper/message.clj" <<'EOF'
(ns jolt-helper.message)

(defn greeting [name]
  (str "Hello, " name "!"))
EOF

cat > "$project/deps.edn" <<'EOF'
{:paths ["src"]
 :deps {local/jolt-helper {:local/root "../jolt-helper"}}}
EOF

cat > "$project/src/jolt_app/main.clj" <<'EOF'
(ns jolt-app.main
  (:require [jolt-app.suffix :as suffix]
            [jolt-helper.message :as message]))

(defn -main [& args]
  (println (suffix/decorate
             (message/greeting (or (first args) "World")))))
EOF

cat > "$project/src/jolt_app/suffix.clj" <<'EOF'
(ns jolt-app.suffix)

(defn decorate [message]
  message)
EOF

bin=$TMP/jolt-app
try "gloat -q -Ejolt -o $bin $project 2>&1"
is "$rc" 0 "gloat -Ejolt builds a deps.edn project"
ok "$([[ -x $bin ]])" "the Jolt output is executable"

if [[ -x $bin ]]; then
  try "$bin Gloat"
  is "$rc" 0 "the Jolt binary exits 0"
  is "$got" "Hello, Gloat!" \
    "the Jolt binary includes local deps and receives arguments"
fi

try "gloat -q -Ejolt --run $project -- Runner 2>&1"
is "$rc" 0 "gloat -Ejolt --run builds and runs a project"
is "$got" "Hello, Runner!" "Jolt --run passes program arguments"

try "printf '%s\n' \
  '(ns main.core) (defn -main [& args] (println (or (first args) \"stdin\")))' |
  gloat -q -Ejolt --run -- stdin-run 2>&1"
is "$rc" 0 "gloat -Ejolt --run builds and runs stdin"
is "$got" "stdin-run" "Jolt stdin receives program arguments"

done-testing
