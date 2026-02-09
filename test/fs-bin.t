#!/usr/bin/env bash

# Test fs/ functions in compiled binary path

source "$(dirname "${BASH_SOURCE[0]}")/init"

# Test fs/mkdir
try "test/call fs/mkdir $TMP/d1"
is "$rc" 0 "fs/mkdir runs"
try "test/call fs-e $TMP/d1"
is "$got" "true" "fs/mkdir created directory"

# Test fs/mkdir-p
try "test/call fs/mkdir-p $TMP/a/b/c"
is "$rc" 0 "fs/mkdir-p runs"
try "test/call fs/dir? $TMP/a/b/c"
is "$got" "true" "fs/mkdir-p created nested dirs"

# Test fs/touch
try "test/call fs/touch $TMP/f1.txt"
is "$rc" 0 "fs/touch runs"
try "test/call fs/file? $TMP/f1.txt"
is "$got" "true" "fs/touch created file"

# Test fs/exists?
try "test/call fs/exists? $TMP/f1.txt"
is "$got" "true" "fs/exists? returns true for existing file"
try "test/call fs/exists? $TMP/nonexistent"
is "$got" "false" "fs/exists? returns false for missing file"

# Test fs/cp
echo "content" > "$TMP/orig.txt"
try "test/call fs/cp $TMP/orig.txt $TMP/copy1.txt"
is "$rc" 0 "fs/cp runs"
try "test/call fs/file? $TMP/copy1.txt"
is "$got" "true" "fs/cp created copy"

# Test fs/mv
echo "move me" > "$TMP/src1.txt"
try "test/call fs/mv $TMP/src1.txt $TMP/dst1.txt"
is "$rc" 0 "fs/mv runs"
try "test/call fs/file? $TMP/dst1.txt"
is "$got" "true" "fs/mv created destination"
try "test/call fs/exists? $TMP/src1.txt"
is "$got" "false" "fs/mv removed source"

# Test fs/rm
echo "delete me" > "$TMP/del1.txt"
try "test/call fs/rm $TMP/del1.txt"
is "$rc" 0 "fs/rm runs"
try "test/call fs/exists? $TMP/del1.txt"
is "$got" "false" "fs/rm deleted file"

# Test fs/rm-r
mkdir -p "$TMP/rmr1/sub"
echo "content" > "$TMP/rmr1/sub/file.txt"
try "test/call fs/rm-r $TMP/rmr1"
is "$rc" 0 "fs/rm-r runs"
try "test/call fs/exists? $TMP/rmr1"
is "$got" "false" "fs/rm-r deleted directory tree"

# Test fs/rm-f
try "test/call fs/rm-f $TMP/nonexistent1.txt"
is "$rc" 0 "fs/rm-f runs without error on missing file"

# Test fs/rmdir
mkdir "$TMP/empty1"
try "test/call fs/rmdir $TMP/empty1"
is "$rc" 0 "fs/rmdir runs"
try "test/call fs/exists? $TMP/empty1"
is "$got" "false" "fs/rmdir deleted empty directory"

# Test fs/cwd
try "test/call fs/cwd"
is "$rc" 0 "fs/cwd runs"
ok "$([[ -n $got ]])" "fs/cwd returns non-empty path"

# Test fs/abs
try "test/call fs/abs ."
is "$rc" 0 "fs/abs runs"
ok "$([[ $got == /* ]])" "fs/abs returns absolute path"

# Test fs/basename
try "test/call fs/basename /path/to/file.txt"
is "$got" "file.txt" "fs/basename extracts filename"

# Test fs/dirname
try "test/call fs/dirname /path/to/file.txt"
is "$got" "/path/to" "fs/dirname extracts directory"

# Test fs/ls
mkdir -p "$TMP/lsdir1"
touch "$TMP/lsdir1/a.txt" "$TMP/lsdir1/b.txt"
try "test/call fs/ls $TMP/lsdir1"
is "$rc" 0 "fs/ls runs"
has "$got" "a.txt" "fs/ls contains first file"
has "$got" "b.txt" "fs/ls contains second file"

done-testing
