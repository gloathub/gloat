#!/bin/bash
# Fix yamlscript.common requirement in ys.std for Babashka compatibility
tmpfile=$1

# Replace yamlscript.common requirement with util requirement
perl -i -pe 's/\[yamlscript\.common :as common :refer \[atom\? re-find\+ regex\?\]\]/[yamlscript.util :as util :refer [atom? re-find+ regex?]]/' "$tmpfile"

# Comment out the curl function that uses ys.http/get
perl -i -pe 's/^\(defn curl \[url\]/#_(defn curl [url]/' "$tmpfile"
