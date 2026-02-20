#!/bin/bash
# Fix read and write functions in ys.fs for Babashka compatibility
tmpfile=$1

# Use perl to replace the functions
# Fix read function
perl -i -pe '
  if (/^\(defn read \[path\]/ .. /^      \(util\/die/) {
    if (/^      \(util\/die/) {
      $_ = qq{(defn read [path]\n  "Read file contents as string"\n  (try\n    (slurp path)\n    (catch Exception e\n      (die "Failed to read file: " path))))\n};
    } else {
      $_ = "";
    }
  }
' "$tmpfile"

# Fix write function
perl -i -pe '
  if (/^\(defn write \[path content\]/ .. /^      \(util\/die/) {
    if (/^      \(util\/die/) {
      $_ = qq{(defn write [path content]\n  "Write string content to file"\n  (try\n    (spit path content)\n    (catch Exception e\n      (die "Failed to write file: " path))))\n};
    } else {
      $_ = "";
    }
  }
' "$tmpfile"
