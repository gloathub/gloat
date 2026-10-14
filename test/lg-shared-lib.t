#!/usr/bin/env bash

# Test let-go shared libraries end to end for both VM-backed engines.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow lg shared library builds. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

case $(uname -s) in
  Darwin) libext=dylib ;;
  *)      libext=so ;;
esac

cat > "$TMP/shared.clj" <<'EOF'
(ns shared.core
  (:require [shared.helper :as helper]))
(def EXPORT {:twice [:int :int]})
(defn twice [x] (helper/twice x))
EOF

cat > "$TMP/helper.clj" <<'EOF'
(ns shared.helper)
(defn twice [x] (* x 2))
EOF

cat > "$TMP/call.c" <<'EOF'
#include <stdio.h>
#include "shared.h"

int main(void) {
  printf("%lld\n", twice(21));
  return 0;
}
EOF

for engine in lgvm lglvm; do
  lib=$TMP/shared.$libext
  try "gloat -q -E$engine -f -o $lib $TMP/shared.clj $TMP/helper.clj 2>&1"
  is "$rc" 0 "gloat -E$engine builds a shared library"
  ok "$([[ -f $lib && -f $TMP/shared.h ]])" \
    "gloat -E$engine generates the library and header"

  try "cc -o $TMP/call-$engine -I$TMP $TMP/call.c $lib"
  is "$rc" 0 "C caller links to the -E$engine library"

  if [[ -x $TMP/call-$engine ]]; then
    # The dylib's install name is the bare basename (shared.dylib). dyld
    # resolves that relative to cwd, so run from $TMP — not from the
    # project root — or load fails with "Library not loaded".
    try "cd $TMP && ./call-$engine"
    is "$rc" 0 "C caller for -E$engine exits 0"
    is "$got" 42 "C caller invokes the -E$engine export"
  fi
done

done-testing
