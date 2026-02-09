#!/usr/bin/env bash

# Test IPC functions in babashka path

source "$(dirname "${BASH_SOURCE[0]}")/init"

# sh: exit code
try "bb test/call.clj sh-exit echo hello"
is "$got" "0" "sh exit 0 on success"

# sh: stdout capture
try "bb test/call.clj sh-stdout echo hello"
is "$got" "hello" "sh captures stdout"

# sh: failure exit code
try "bb test/call.clj sh-exit false"
is "$got" "1" "sh exit 1 on failure"

# sh-out
try "bb test/call.clj sh-out echo hello"
is "$got" "hello" "sh-out returns trimmed stdout"

# Note: shell function behaves differently in babashka (streams vs strings)
# so we skip shell tests in babashka path

# bash-out
try "bb test/call.clj bash-out echo hello"
is "$got" "hello" "bash-out returns stdout"

done-testing
