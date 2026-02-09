#!/usr/bin/env bash

# Test global variables in compiled binary

source "$(dirname "${BASH_SOURCE[0]}")/init"

# ENV: environment variables
try "test/call env-home"
like "$got" "^/" "ENV.HOME returns home directory path"

try "test/call env-path-len"
like "$got" "^[1-9][0-9]*$" "ENV.PATH has length > 0"

# NS: namespace
try "test/call ns"
like "$got" "call.core" "NS returns namespace name"

# CWD: current working directory
try "test/call cwd"
is "$got" "$PWD" "CWD returns current working directory"

# FILE: absolute source file path
try "test/call file"
like "$got" "/test/call.ys$" "FILE returns absolute source path"

# DIR: directory containing source file
try "test/call dir"
like "$got" "/test$" "DIR returns source directory"

# RUN.pid: process ID
try "test/call run-pid"
like "$got" "^[0-9]+$" "RUN.pid returns process ID"

# RUN.args: command-line arguments (empty - just test name)
try "test/call run-args-empty"
is "$got" '[run-args-empty]' "RUN.args contains test name only"

# RUN.args: command-line arguments (with args)
try "test/call run-args-with arg1 arg2"
is "$got" '(arg1 arg2)' "RUN.args.rest() excludes test name"

# ARGV: raw arguments (empty - just test name)
try "test/call argv-empty"
is "$got" '(argv-empty)' "ARGV contains test name only"

# ARGV: raw arguments (with args)
try "test/call argv-with arg1 arg2"
is "$got" '(arg1 arg2)' "ARGV.rest() excludes test name"

# ARGS: parsed arguments (empty - just test name)
try "test/call args-empty"
is "$got" '[args-empty]' "ARGS contains test name only"

# ARGS: parsed arguments (with args)
try "test/call args-with arg1 arg2"
is "$got" '(arg1 arg2)' "ARGS.rest() excludes test name"

done-testing
