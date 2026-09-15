#!/usr/bin/env bash

# shellcheck source=test/init
source "$(dirname "${BASH_SOURCE[0]}")/init"

rc=
got=

try "cd '$PROJECT_ROOT' && bb test/browser_test.clj"
is "$rc" 0 "browser bundle helpers pass"

try "cd '$PROJECT_ROOT' && node test/browser_runner_test.cjs"
is "$rc" 0 "browser stream runner passes"

for extension in html serve open; do
  try "$PROJECT_ROOT/bin/gloat -t js -X$extension=value missing.ys"
  is "$rc" 1 "-X$extension rejects extension arguments"
  has "$got" "must be specified in the URL query" \
    "-X$extension directs arguments to the URL query"
done

done-testing
