#!/bin/bash
# Fix read and write functions in ys.fs for Babashka compatibility
tmpfile=$1

# Use sed to replace the functions
# Fix read function
sed -i '/^(defn read \[path\]/,/^      (util\/die/c\
(defn read [path]\
  "Read file contents as string"\
  (try\
    (slurp path)\
    (catch Exception e\
      (die "Failed to read file: " path))))' "$tmpfile"

# Fix write function
sed -i '/^(defn write \[path content\]/,/^      (util\/die/c\
(defn write [path content]\
  "Write string content to file"\
  (try\
    (spit path content)\
    (catch Exception e\
      (die "Failed to write file: " path))))' "$tmpfile"
