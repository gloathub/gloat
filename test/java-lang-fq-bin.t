#!/usr/bin/env bash

# Test fully-qualified java.lang.X/y interop in a gloat-compiled binary.
# In standard Clojure, java.lang.* classes are auto-imported, so the
# fully-qualified form must resolve identically to the bare form. This
# verifies the rewrite step strips the java.lang. prefix before mapping
# Class/member symbols to the javacompat bridge.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow java.lang.* FQ binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/java-lang-fq-bin
fixture=$PROJECT_ROOT/test/fixtures/java-lang-fq.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds java-lang-fq fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check math-abs   '42'                  'java.lang.Math/abs'
check math-sqrt  '4.0'                 'java.lang.Math/sqrt'
check math-pi    '3.141592653589793'   'java.lang.Math/PI'
check math-pow   '1024.0'              'java.lang.Math/pow'

check int-parse  '42'                  'java.lang.Integer/parseInt'
check int-max    '2147483647'          'java.lang.Integer/MAX_VALUE'
check int-bin    '101010'              'java.lang.Integer/toBinaryString'
check int-ctor   '100'                 '(java.lang.Integer. n)'

check long-parse '9999999999'          'java.lang.Long/parseLong'
check long-max   '9223372036854775807' 'java.lang.Long/MAX_VALUE'
check long-ctor  '123456789012345'     '(java.lang.Long. n)'

check sys-line   '1'                   'java.lang.System/lineSeparator'
check sys-env    'true'                'java.lang.System/getenv'

done-testing
