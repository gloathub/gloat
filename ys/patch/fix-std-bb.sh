#!/bin/bash
# Fix yamlscript.common requirement in ys.std for Babashka compatibility
tmpfile=$1

# Replace yamlscript.common requirement with util requirement
sed -i 's/\[yamlscript\.common :as common :refer \[atom? re-find+ regex?\]\]/[yamlscript.util :as util :refer [atom? re-find+ regex?]]/' "$tmpfile"
# Remove the yamlscript.util line that's now redundant
sed -i '/^\   \[yamlscript\.util :as util\]$/d' "$tmpfile"

# Comment out the curl function that uses ys.http/get
sed -i 's/^(defn curl \[url\]/#_(defn curl [url]/' "$tmpfile"
