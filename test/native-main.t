#!/usr/bin/env bash

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping native shared library builds. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

case $(uname -s) in
  Darwin) libext=dylib ;;
  *) libext=so ;;
esac

mkdir "$TMP/native"
cat > "$TMP/shared.clj" <<'CLJ'
(ns shared.core)
(def EXPORT {:twice [:int :int]})
(defn twice [x] (* x 2))
CLJ
cat > "$TMP/native/extra.h" <<'HEADER'
int native_answer(void);
HEADER
cat > "$TMP/native/extra.c" <<'C'
#include "extra.h"
int native_answer(void) { return 42; }
C
cat > "$TMP/native/extra.go" <<'GO'
package main
/*
#include "extra.h"
*/
import "C"
GO
cat > "$TMP/call.c" <<'C'
#include <assert.h>
#include "shared.h"
#ifdef WITH_NATIVE
#include "extra.h"
#endif
int main(void) {
    assert(twice(21) == 42);
#ifdef WITH_NATIVE
    assert(native_answer() == 42);
#endif
    return 0;
}
C

try "gloat -q -Eglj -f $TMP/shared.clj -o $TMP/shared.$libext"
is "$rc" 0 'Ordinary shared library still builds'
try "cc -I$TMP $TMP/call.c $TMP/shared.$libext -o $TMP/call"
is "$rc" 0 'Ordinary generated header links'
try "$TMP/call"
is "$rc" 0 'Ordinary export works'

export GLOAT_EXTRA_GO_MAIN_DIR=$TMP/native
try "gloat -q -Eglj -f $TMP/shared.clj -o $TMP/shared.$libext"
is "$rc" 0 'Native main-package files build'
try "cc -DWITH_NATIVE -I$TMP -I$TMP/native $TMP/call.c $TMP/shared.$libext -o $TMP/call"
is "$rc" 0 'Custom C export links'
try "$TMP/call"
is "$rc" 0 'Custom and generated exports work together'

cat > "$TMP/empty.clj" <<'CLJ'
(ns empty.core)
(def EXPORT {})
CLJ
cat > "$TMP/helper.clj" <<'CLJ'
(ns helper.core)
CLJ
try "gloat -Eglj -f $TMP/helper.clj $TMP/empty.clj -o $TMP/empty.$libext 2>&1"
is "$rc" 0 'Empty EXPORT supports handwritten exports'
has "$got" 'Main namespace: empty.core' 'Empty EXPORT selects the entrypoint'

cp "$TMP/native/extra.go" "$TMP/native/main.go"
try "gloat -q -Eglj -f $TMP/shared.clj -o $TMP/shared.$libext 2>&1"
is "$rc" 1 'Generated filename collision fails'
has "$got" 'Native main-package filename collision: main.go' \
  'Collision identifies the filename'

done-testing
