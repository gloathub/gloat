#!/usr/bin/env bash

# Test java.util.UUID interop in a gloat-compiled binary. Verifies UUID/*
# statics, the (UUID. msb lsb) constructor, instance methods reached through
# reflective FieldOrMethod dispatch, and the fully qualified java.util.UUID/*
# resolution.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow UUID/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/uuid-bin
fixture=$PROJECT_ROOT/test/fixtures/uuid.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds uuid fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check random-version  '4'                                      'UUID/randomUUID version is 4'
check random-variant  '2'                                      'UUID/randomUUID variant is IETF'
check random-format   'true'                                   'UUID/randomUUID toString matches v4 layout'
check from-string     '01234567-89ab-cdef-0123-456789abcdef'   'UUID/fromString round-trips'
check msb             '42'                                     'UUID.getMostSignificantBits decodes high half'
check lsb             '42'                                     'UUID.getLeastSignificantBits decodes low half'
check named-version   '3'                                      'UUID/nameUUIDFromBytes version is 3'
check named-string    '5d41402a-bc4b-3a76-b971-9d911017c592'   'MD5(hello) UUID matches JVM output'
check ctor-string     '00000000-0000-002a-0000-000000000063'   '(UUID. 42 99) toString'
check ctor-msb        '42'                                     '(UUID. 42 99) getMostSignificantBits'
check ctor-lsb        '99'                                     '(UUID. 42 99) getLeastSignificantBits'
check equals-true     'true'                                   'UUID.equals same bits'
check equals-false    'false'                                  'UUID.equals different bits'
check compare-eq      '0'                                      'UUID.compareTo equal'
check compare-lt      '-1'                                     'UUID.compareTo lesser is -1'
check compare-gt      '1'                                      'UUID.compareTo greater is 1'
check hashcode-stable 'true'                                   'UUID.hashCode stable across parses'
check fq-random       'true'                                   'java.util.UUID/randomUUID still produces UUID-shaped string'
check fq-fromstring   '01234567-89ab-cdef-0123-456789abcdef'   'java.util.UUID/fromString resolves through bridge'

done-testing
