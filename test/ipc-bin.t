#!/usr/bin/env bash

# Test IPC functions in compiled binary path

source "$(dirname "${BASH_SOURCE[0]}")/init"

# sh: exit code
try "test/call sh-exit echo hello"
is "$got" "0" "sh exit 0 on success"

# sh: stdout capture
try "test/call sh-stdout echo hello"
is "$got" "hello" "sh captures stdout"

# shell: stderr capture
try "test/call shell-stderr 'echo error >&2'"
is "$got" "error" "shell captures stderr"

# sh: failure exit code
try "test/call sh-exit false"
is "$got" "1" "sh exit 1 on failure"

# sh-out
try "test/call sh-out echo hello"
is "$got" "hello" "sh-out returns trimmed stdout"

# shell: runs through /bin/sh -c
try "test/call shell-stdout echo hello"
is "$got" "hello" "shell captures stdout"

# shell: supports pipes
try "test/call shell-stdout 'echo hello | tr h H'"
is "$got" "Hello" "shell supports pipes"

# bash-out
try "test/call bash-out echo hello"
is "$got" "hello" "bash-out returns stdout"

done-testing
