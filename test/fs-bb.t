#!/usr/bin/env bash

# Test fs/ functions in babashka path

source "$(dirname "${BASH_SOURCE[0]}")/init"

# Test fs/mkdir
try "bb test/call.clj fs/mkdir $TMP/d2"
is "$rc" 0 "fs/mkdir runs"
try "bb test/call.clj fs-e $TMP/d2"
is "$got" "true" "fs/mkdir created directory"

# Test fs/mkdir-p
try "bb test/call.clj fs/mkdir-p $TMP/x/y/z"
is "$rc" 0 "fs/mkdir-p runs"
try "bb test/call.clj fs/dir? $TMP/x/y/z"
is "$got" "true" "fs/mkdir-p created nested dirs"

# Test fs/touch
try "bb test/call.clj fs/touch $TMP/f2.txt"
is "$rc" 0 "fs/touch runs"
try "bb test/call.clj fs/file? $TMP/f2.txt"
is "$got" "true" "fs/touch created file"

# Test fs/exists?
try "bb test/call.clj fs/exists? $TMP/f2.txt"
is "$got" "true" "fs/exists? returns true for existing file"
try "bb test/call.clj fs/exists? $TMP/nonexistent"
is "$got" "false" "fs/exists? returns false for missing file"

# Test fs/cp
echo "content" > "$TMP/orig.txt"
try "bb test/call.clj fs/cp $TMP/orig.txt $TMP/copy2.txt"
is "$rc" 0 "fs/cp runs"
try "bb test/call.clj fs/file? $TMP/copy2.txt"
is "$got" "true" "fs/cp created copy"

# Test fs/mv
echo "move me" > "$TMP/src2.txt"
try "bb test/call.clj fs/mv $TMP/src2.txt $TMP/dst2.txt"
is "$rc" 0 "fs/mv runs"
try "bb test/call.clj fs/file? $TMP/dst2.txt"
is "$got" "true" "fs/mv created destination"
try "bb test/call.clj fs/exists? $TMP/src2.txt"
is "$got" "false" "fs/mv removed source"

# Test fs/rm
echo "delete me" > "$TMP/del2.txt"
try "bb test/call.clj fs/rm $TMP/del2.txt"
is "$rc" 0 "fs/rm runs"
try "bb test/call.clj fs/exists? $TMP/del2.txt"
is "$got" "false" "fs/rm deleted file"

# Test fs/rm-r
mkdir -p "$TMP/rmr2/sub"
echo "content" > "$TMP/rmr2/sub/file.txt"
try "bb test/call.clj fs/rm-r $TMP/rmr2"
is "$rc" 0 "fs/rm-r runs"
try "bb test/call.clj fs/exists? $TMP/rmr2"
is "$got" "false" "fs/rm-r deleted directory tree"

# Test fs/rm-f
try "bb test/call.clj fs/rm-f $TMP/nonexistent2.txt"
is "$rc" 0 "fs/rm-f runs without error on missing file"

# Test fs/rmdir
mkdir "$TMP/empty2"
try "bb test/call.clj fs/rmdir $TMP/empty2"
is "$rc" 0 "fs/rmdir runs"
try "bb test/call.clj fs/exists? $TMP/empty2"
is "$got" "false" "fs/rmdir deleted empty directory"

# Test fs/cwd
try "bb test/call.clj fs/cwd"
is "$rc" 0 "fs/cwd runs"
ok "$([[ -n $got ]])" "fs/cwd returns non-empty path"

# Test fs/abs
try "bb test/call.clj fs/abs ."
is "$rc" 0 "fs/abs runs"
ok "$([[ $got == /* ]])" "fs/abs returns absolute path"

# Test fs/basename
try "bb test/call.clj fs/basename /path/to/file.txt"
is "$got" "file.txt" "fs/basename extracts filename"

# Test fs/dirname
try "bb test/call.clj fs/dirname /path/to/file.txt"
is "$got" "/path/to" "fs/dirname extracts directory"

# Test fs/ls
mkdir -p "$TMP/lsdir2"
touch "$TMP/lsdir2/x.txt" "$TMP/lsdir2/y.txt"
try "bb test/call.clj fs/ls $TMP/lsdir2"
is "$rc" 0 "fs/ls runs"
has "$got" "x.txt" "fs/ls contains first file"
has "$got" "y.txt" "fs/ls contains second file"

done-testing
