#!/usr/bin/env bash

# Test global variables in babashka format
# Note: ENV, NS, CWD, and RUN are not initialized in bb format (no Go runtime)
# Only FILE, DIR, ARGV, and ARGS are available

source "$(dirname "${BASH_SOURCE[0]}")/init"

# FILE: absolute source file path (shows .ys source, not .clj output)
try "bb test/call.clj file"
like "$got" "/test/call\.(ys|clj)$" "FILE returns absolute source path"

# DIR: directory containing source file
try "bb test/call.clj dir"
like "$got" "/test$" "DIR returns source directory"

# ARGV: raw arguments (empty - just test name)
try "bb test/call.clj argv-empty"
is "$got" '(argv-empty)' "ARGV contains test name only"

# ARGV: raw arguments (with args)
try "bb test/call.clj argv-with arg1 arg2"
is "$got" '(arg1 arg2)' "ARGV.rest() excludes test name"

# ARGS: parsed arguments (empty - just test name)
try "bb test/call.clj args-empty"
is "$got" '[args-empty]' "ARGS contains test name only"

# ARGS: parsed arguments (with args)
try "bb test/call.clj args-with arg1 arg2"
is "$got" '(arg1 arg2)' "ARGS.rest() excludes test name"

done-testing
