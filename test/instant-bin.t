#!/usr/bin/env bash

# Test java.time.Instant interop in a gloat-compiled binary. Verifies the
# Instant/* statics, instance methods reached through reflective FieldOrMethod
# dispatch, and the fully qualified java.time.Instant/* resolution.

source "$(dirname "${BASH_SOURCE[0]}")/init"

if [[ -z ${RUN_SLOW_TESTS:-} ]]; then
  pass 'Skipping slow Instant/* binary build. Try RUN_SLOW_TESTS=1.'
  done-testing
  exit 0
fi

bin=$TMP/instant-bin
fixture=$PROJECT_ROOT/test/fixtures/instant.clj

try "gloat -q -o $bin $fixture 2>&1"
is "$rc" 0 "gloat -o builds instant fixture binary"
ok "$([[ -x $bin ]])" "gloat produced an executable"

[[ -x $bin ]] || { done-testing; exit 0; }

check() {
  local op=$1 expected=$2 desc=$3
  try "$bin $op"
  is "$got" "$expected" "$desc"
}

check epoch-string     '1970-01-01T00:00:00Z'           'Instant/EPOCH toString'
check epoch-seconds    '0'                              'Instant/EPOCH getEpochSecond'
check epoch-nano       '0'                              'Instant/EPOCH getNano'
check now-shape        'true'                           'Instant/now toString matches ISO-8601 form'
check parse-no-frac    '2007-12-03T10:15:30Z'           'Instant/parse without subsecond'
check parse-millis     '2007-12-03T10:15:30.500Z'       'Instant/parse with millisecond fraction'
check parse-nanos      '2007-12-03T10:15:30.500000123Z' 'Instant/parse with nanosecond fraction'
check epoch-second-1   '2007-12-03T10:15:30Z'           'Instant/ofEpochSecond (1-arg)'
check epoch-second-2   '1970-01-01T00:00:01.500Z'       'Instant/ofEpochSecond (millis nano-adj)'
check epoch-milli      '2007-12-03T10:15:30.500Z'       'Instant/ofEpochMilli'
check to-epoch-milli   '1196676930500'                  'Instant.toEpochMilli round-trip'
check get-seconds      '1196676930'                     'Instant.getEpochSecond'
check get-nano         '500000000'                      'Instant.getNano'
check plus-seconds     '2007-12-03T10:15:35Z'           'Instant.plusSeconds'
check plus-millis      '2007-12-03T10:15:32Z'           'Instant.plusMillis crosses second boundary'
check plus-nanos       '2007-12-03T10:15:30.000000001Z' 'Instant.plusNanos'
check minus-seconds    '2007-12-03T10:15:25Z'           'Instant.minusSeconds'
check minus-millis     '2007-12-03T10:15:30.250Z'       'Instant.minusMillis'
check is-before        'true'                           'Instant.isBefore'
check is-after         'true'                           'Instant.isAfter'
check compare-eq       '0'                              'Instant.compareTo equal'
check compare-lt       '-1'                             'Instant.compareTo lesser is -1'
check compare-gt       '1'                              'Instant.compareTo greater is 1'
check equals-true      'true'                           'Instant.equals same'
check equals-false     'false'                          'Instant.equals different'
check hashcode-stable  'true'                           'Instant.hashCode stable across parses'
check fq-now-shape     'true'                           'java.time.Instant/now resolves through bridge'
check fq-parse         '2007-12-03T10:15:30Z'           'java.time.Instant/parse resolves through bridge'

done-testing
