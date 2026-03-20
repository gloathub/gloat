;; report.clj - Binary size analysis for gloat
;;
;; Loaded by gloat.clj via load-file.
;; Provides report/generate-report for the -Xreport extension.

(ns report
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]))

;;------------------------------------------------------------------------------
;; Analysis
;;------------------------------------------------------------------------------

(defn parse-readelf-sections
  "Parse readelf -SW output into a seq of {:name :type :size :nobits?}."
  [binary]
  (let [result (process/shell {:out :string :err :string} "readelf" "-SW" binary)
        lines (str/split-lines (:out result))]
    (->> lines
         (keep (fn [line]
                 ;; Format: [Nr] Name Type Address Off Size ES Flg ...
                 (when-let [[_ name type size]
                            (re-find #"\]\s+(\S+)\s+(PROGBITS|NOBITS)\s+[0-9a-f]+\s+[0-9a-f]+\s+([0-9a-f]+)"
                                     line)]
                   {:name name
                    :type type
                    :size (Long/parseLong size 16)
                    :nobits? (= type "NOBITS")})))
         (filter #(pos? (:size %))))))

(defn parse-nm-symbols
  "Parse go tool nm -size output into a seq of {:size :type :name}."
  [go-bin binary]
  (let [result (process/shell {:out :string :err :string} go-bin "tool" "nm" "-size" binary)
        lines (str/split-lines (:out result))]
    (->> lines
         (keep (fn [line]
                 (when-let [[_ _addr size-s sym-type sym-name]
                            (re-find #"^\s*([0-9a-f]+)\s+(\d+)\s+(\S)\s+(.+)$"
                                     line)]
                   (let [size (Long/parseLong size-s)]
                     (when (pos? size)
                       {:size size
                        :type sym-type
                        :name sym-name})))))
         (remove #(str/starts-with? (:name %) "local.")))))

(defn categorize-symbol
  "Assign a category to a symbol name."
  [sym]
  (cond
    (str/starts-with? sym "go:func")                    "go:func-table"
    (str/starts-with? sym "type:")                      "go:type-table"
    (re-find #"^github\.com/.*/gljimports" sym)         "glojure/gljimports"
    (re-find #"^github\.com/.*/glojure/pkg/stdlib" sym) "glojure/stdlib"
    (re-find #"^github\.com/.*/glojure" sym)            "glojure/runtime"
    (re-find #"/internal/stdlib/" sym)                  "user/clojure.core"
    (re-find #"^github\.com/" sym)                      "user/code"
    (str/starts-with? sym "runtime")                    "go-runtime"
    (str/starts-with? sym "crypto")                     "go-stdlib/crypto"
    (str/starts-with? sym "net")                        "go-stdlib/net+http"
    (re-find #"^go/" sym)                               "go-stdlib/go-tools"
    (str/starts-with? sym "encoding")                   "go-stdlib/encoding"
    (str/starts-with? sym "testing")                    "go-stdlib/testing"
    (str/starts-with? sym "database")                   "go-stdlib/database"
    (str/starts-with? sym "slices")                     "go-stdlib/slices"
    (str/starts-with? sym "vendor")                     "go-stdlib/vendor"
    :else                                               "go-stdlib/other"))

(defn extract-package
  "Extract the Go package path from a symbol name."
  [sym]
  (let [sym (str/replace sym #"\[.*?\]" "")
        sym (str/replace sym #"\.\(\*?[^)]+\)\..+$" "")
        parts (str/split sym #"/")
        n (count parts)]
    (if (= n 1)
      (first (str/split sym #"\." 2))
      (let [last-part (last parts)
            dot-idx (str/index-of last-part ".")]
        (if dot-idx
          (str/join "/" (concat (butlast parts)
                                [(subs last-part 0 dot-idx)]))
          (str/join "/" parts))))))

;;------------------------------------------------------------------------------
;; Formatting
;;------------------------------------------------------------------------------

(defn fmt-size [n]
  (cond
    (>= n (* 1024 1024)) (format "%.1f MB" (/ n 1048576.0))
    (>= n 1024)          (format "%.1f KB" (/ n 1024.0))
    :else                 (format "%d B" n)))

(defn fmt-pct [n total]
  (format "%.1f%%" (* 100.0 (/ n total))))

(defn- go-version-short
  "Extract just the version string (e.g. 'go1.26.1') from 'go version go1.26.1 linux/amd64'."
  [go-version-str]
  (or (second (re-find #"(go\S+)" go-version-str))
      go-version-str))

(defn- ensure-v-prefix [s]
  (when s
    (if (str/starts-with? s "v") s (str "v" s))))

(defn- platform-str
  "Determine OS/ARCH string from explicit goos/goarch or go version output."
  [goos goarch go-version-str]
  (if (and goos goarch)
    (str goos "/" goarch)
    (or (second (re-find #"\s(\S+/\S+)\s*$" go-version-str))
        "unknown")))

(defn exclude-fips-bss [symbols]
  (remove #(= (:name %) "crypto/internal/fips140/drbg.memory") symbols))

;;------------------------------------------------------------------------------
;; Markdown Report Generation
;;------------------------------------------------------------------------------

(defn- md-section-table [sections file-size]
  (let [key-names [".text" ".rodata" ".gopclntab" ".noptrdata" ".data"]
        section-map (into {} (map (juxt :name identity) sections))
        disk-total (reduce + (map :size (remove :nobits? sections)))
        overhead (- file-size disk-total)
        bss-sections (filter :nobits? sections)]
    (str "## On-Disk Section Breakdown\n\n"
         "| Section | Size | % |\n"
         "|---------|------|---|\n"
         (apply str
                (for [name key-names
                      :let [s (section-map name)]
                      :when s]
                  (format "| `%s` | %s | %s |\n"
                          (:name s) (fmt-size (:size s))
                          (fmt-pct (:size s) file-size))))
         (format "| ELF overhead | %s | %s |\n"
                 (fmt-size overhead) (fmt-pct overhead file-size))
         (format "| **TOTAL** | **%s** | |\n" (fmt-size file-size))
         (when (seq bss-sections)
           (str "\n"
                (apply str
                       (for [s bss-sections]
                         (format "%s: %s in RAM, 0 bytes on disk\n"
                                 (:name s) (fmt-size (:size s))))))))))

(defn- md-category-table [symbols]
  (let [symbols (exclude-fips-bss symbols)
        by-cat (->> symbols
                    (group-by #(categorize-symbol (:name %)))
                    (map (fn [[cat syms]]
                           {:category cat
                            :size (reduce + (map :size syms))}))
                    (sort-by :size >))
        total (reduce + (map :size by-cat))]
    (str "## Code + Data by Category\n\n"
         "| Category | Size | % |\n"
         "|----------|------|---|\n"
         (apply str
                (for [{:keys [category size]} by-cat]
                  (format "| %s | %s | %s |\n"
                          category (fmt-size size)
                          (fmt-pct size total))))
         (format "| **TOTAL** | **%s** | |\n" (fmt-size total)))))

(defn- md-package-table [symbols top-n]
  (let [symbols (exclude-fips-bss symbols)
        by-pkg (->> symbols
                    (group-by #(extract-package (:name %)))
                    (map (fn [[pkg syms]]
                           {:package pkg
                            :size (reduce + (map :size syms))}))
                    (sort-by :size >)
                    (take top-n))
        total (reduce + (map :size symbols))]
    (str "## Top " top-n " Packages by Size\n\n"
         "| Package | Size | % |\n"
         "|---------|------|---|\n"
         (apply str
                (for [{:keys [package size]} by-pkg]
                  (format "| `%s` | %s | %s |\n"
                          package (fmt-size size)
                          (fmt-pct size total)))))))

(defn- md-symbol-table [symbols top-n]
  (let [symbols (exclude-fips-bss symbols)
        top (->> symbols
                 (sort-by :size >)
                 (take top-n))]
    (str "## Top " top-n " Individual Symbols\n\n"
         "| Size | Type | Symbol |\n"
         "|------|------|--------|\n"
         (apply str
                (for [{:keys [size type name]} top]
                  (format "| %s | %s | `%s` |\n"
                          (fmt-size size) type name))))))

;;------------------------------------------------------------------------------
;; HTML Report Generation
;;------------------------------------------------------------------------------

(defn- html-escape [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- source-lang [filename]
  (let [ext (last (str/split filename #"\."))]
    (case ext
      "ys"  "yaml"
      "clj" "clojure"
      "glj" "clojure"
      "go"  "go"
      ext)))

(defn- html-source-section [sources]
  (when (seq sources)
    (str "<h2>Source Files</h2>\n"
         (apply str
                (for [{:keys [name content]} sources
                      :let [lang (source-lang name)]]
                  (str "<details>\n"
                       "<summary>" (html-escape name) "</summary>\n"
                       "<pre><code class=\"language-" lang "\">"
                       (html-escape content) "</code></pre>\n"
                       "</details>\n"))))))

(defn- html-cell [[display sort-val]]
  (if sort-val
    (format "<td data-sort=\"%s\">%s</td>" sort-val display)
    (str "<td>" display "</td>")))

(defn- html-row [row]
  (str "<tr>"
       (apply str (map (fn [cell]
                         (if (vector? cell)
                           (html-cell cell)
                           (str "<td>" cell "</td>")))
                       row))
       "</tr>\n"))

(defn- html-sortable-table [id headers rows & [footer-row]]
  (str "<table id=\"" id "\">\n"
       "<thead><tr>"
       (apply str (map-indexed
                   (fn [i h]
                     (format "<th onclick=\"sortTable('%s',%d)\">%s ⇅</th>" id i h))
                   headers))
       "</tr></thead>\n"
       "<tbody>\n"
       (apply str (map html-row rows))
       "</tbody>\n"
       (when footer-row
         (str "<tfoot>\n" (html-row footer-row) "</tfoot>\n"))
       "</table>\n"))

(defn- html-section-table [sections file-size]
  (let [key-names [".text" ".rodata" ".gopclntab" ".noptrdata" ".data"]
        section-map (into {} (map (juxt :name identity) sections))
        disk-total (reduce + (map :size (remove :nobits? sections)))
        overhead (- file-size disk-total)
        bss-sections (filter :nobits? sections)
        rows (concat
              (for [name key-names
                    :let [s (section-map name)]
                    :when s]
                [(str "<code>" (:name s) "</code>")
                 [(fmt-size (:size s)) (str (:size s))]
                 (format "%.1f" (* 100.0 (/ (:size s) file-size)))
                 (str (:size s))])
              [["ELF overhead"
                [(fmt-size overhead) (str overhead)]
                (format "%.1f" (* 100.0 (/ overhead file-size)))
                (str overhead)]])
        footer [(str "<strong>TOTAL</strong>")
                [(str "<strong>" (fmt-size file-size) "</strong>") (str file-size)]
                ""
                (str file-size)]]
    (str "<h2>On-Disk Section Breakdown</h2>\n"
         (html-sortable-table "sections" ["Section" "Size" "%" "Bytes"] rows footer)
         (when (seq bss-sections)
           (str "<p>"
                (apply str
                       (for [s bss-sections]
                         (format "%s: %s in RAM, 0 bytes on disk<br>"
                                 (:name s) (fmt-size (:size s)))))
                "</p>\n")))))

(defn- html-category-table [symbols]
  (let [symbols (exclude-fips-bss symbols)
        by-cat (->> symbols
                    (group-by #(categorize-symbol (:name %)))
                    (map (fn [[cat syms]]
                           {:category cat
                            :size (reduce + (map :size syms))}))
                    (sort-by :size >))
        total (reduce + (map :size by-cat))
        rows (for [{:keys [category size]} by-cat]
               [(html-escape category)
                [(fmt-size size) (str size)]
                (format "%.1f" (* 100.0 (/ size total)))
                (str size)])
        footer [(str "<strong>TOTAL</strong>")
                [(str "<strong>" (fmt-size total) "</strong>") (str total)]
                ""
                (str total)]]
    (str "<h2>Code + Data by Category</h2>\n"
         (html-sortable-table "categories" ["Category" "Size" "%" "Bytes"] rows footer))))

(defn- html-package-table [symbols top-n]
  (let [symbols (exclude-fips-bss symbols)
        by-pkg (->> symbols
                    (group-by #(extract-package (:name %)))
                    (map (fn [[pkg syms]]
                           {:package pkg
                            :size (reduce + (map :size syms))}))
                    (sort-by :size >)
                    (take top-n))
        total (reduce + (map :size symbols))
        rows (for [{:keys [package size]} by-pkg]
               [(str "<code>" (html-escape package) "</code>")
                [(fmt-size size) (str size)]
                (format "%.1f" (* 100.0 (/ size total)))
                (str size)])]
    (str "<h2>Top " top-n " Packages by Size</h2>\n"
         (html-sortable-table "packages" ["Package" "Size" "%" "Bytes"] rows))))

(defn- html-symbol-table [symbols top-n]
  (let [symbols (exclude-fips-bss symbols)
        top (->> symbols
                 (sort-by :size >)
                 (take top-n))
        rows (for [{:keys [size type name]} top]
               [[(fmt-size size) (str size)]
                (str size)
                type
                (str "<code>" (html-escape name) "</code>")])]
    (str "<h2>Top " top-n " Individual Symbols</h2>\n"
         (html-sortable-table "symbols" ["Size" "Bytes" "Type" "Symbol"] rows))))

(def ^:private html-sort-script
  "<script>
function sortTable(id, col) {
  var table = document.getElementById(id);
  var tbody = table.tBodies[0];
  var rows = Array.from(tbody.rows);
  var nums = 0, total = 0;
  rows.forEach(function(r) {
    var c = r.cells[col];
    var v = (c.dataset.sort || c.textContent).trim();
    if (v !== '') { total++; if (/^[0-9.]+$/.test(v)) nums++; }
  });
  var isNum = total > 0 && nums > total / 2;
  function val(r) {
    var c = r.cells[col];
    return c.dataset.sort || c.textContent.trim();
  }
  var sameCol = table.dataset.sortCol == col;
  var asc;
  if (sameCol) {
    asc = table.dataset.sortAsc != 'true';
  } else if (isNum) {
    var desc = true;
    for (var i = 1; i < rows.length; i++) {
      var prev = parseFloat(val(rows[i-1]));
      var curr = parseFloat(val(rows[i]));
      if (!isNaN(prev) && !isNaN(curr) && curr > prev) { desc = false; break; }
    }
    asc = desc;
  } else {
    asc = true;
  }
  rows.sort(function(a, b) {
    var av = val(a), bv = val(b);
    var an = parseFloat(av), bn = parseFloat(bv);
    if (!isNaN(an) && !isNaN(bn)) return asc ? an - bn : bn - an;
    return asc ? av.localeCompare(bv) : bv.localeCompare(av);
  });
  rows.forEach(function(r) { tbody.appendChild(r); });
  table.dataset.sortCol = col;
  table.dataset.sortAsc = asc;
}
</script>")

(def ^:private html-style
  "<style>
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif; max-width: 960px; margin: 2em auto; padding: 0 1em; color: #24292f; }
h1 { border-bottom: 1px solid #d0d7de; padding-bottom: 0.3em; }
h2 { margin-top: 1.5em; }
table { border-collapse: collapse; width: 100%; margin: 0.5em 0; }
table.summary { width: auto; }
th, td { border: 1px solid #d0d7de; padding: 6px 12px; text-align: left; }
th { background: #f6f8fa; cursor: pointer; user-select: none; white-space: nowrap; }
th:hover { background: #eaeef2; }
tr:nth-child(even) { background: #f6f8fa; }
tfoot td { font-weight: bold; }
code { background: #f0f3f6; padding: 0.2em 0.4em; border-radius: 3px; font-size: 0.9em; }
pre { background: #f6f8fa; padding: 1em; overflow-x: auto; border-radius: 6px; }
pre code.hljs { background: inherit; padding: 0; }
details { margin: 0.5em 0; }
summary { cursor: pointer; font-weight: 600; }
</style>")

(defn- generate-html-report
  [{:keys [stripped-binary unstripped-binary go-bin report-path
           output-name goos goarch gloat-version glojure-version
           pruned sources top-n]
    :or {top-n 30}}]
  (let [go-version-raw (str/trim (:out (process/shell {:out :string :err :string}
                                                      go-bin "version")))
        go-ver (go-version-short go-version-raw)
        platform (platform-str goos goarch go-version-raw)
        stripped-name (or output-name (fs/file-name stripped-binary))
        unstripped-name (str stripped-name "-unstripped")
        stripped-size (fs/size stripped-binary)
        unstripped-size (fs/size unstripped-binary)
        sections (parse-readelf-sections stripped-binary)
        symbols (parse-nm-symbols go-bin unstripped-binary)
        report (str "<!DOCTYPE html>\n<html>\n<head>\n"
                    "<meta charset=\"utf-8\">\n"
                    "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/github.min.css\">\n"
                    "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js\"></script>\n"
                    "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/yaml.min.js\"></script>\n"
                    "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/clojure.min.js\"></script>\n"
                    "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/go.min.js\"></script>\n"
                    "<title>Binary Size Analysis</title>\n"
                    html-style "\n"
                    "</head>\n<body>\n"
                    "<h1>Binary Size Analysis</h1>\n"
                    "<table class=\"summary\">\n"
                    (format "<tr><td><code>%s</code></td><td>%s</td></tr>\n" (html-escape stripped-name) (fmt-size stripped-size))
                    (format "<tr><td><code>%s</code></td><td>%s</td></tr>\n" (html-escape unstripped-name) (fmt-size unstripped-size))
                    (format "<tr><td><code>-Xprune</code></td><td>%s</td></tr>\n" (if pruned "true" "false"))
                    (format "<tr><td>Gloat</td><td>%s</td></tr>\n" (html-escape (or (ensure-v-prefix gloat-version) "unknown")))
                    (format "<tr><td>Glojure</td><td>%s</td></tr>\n" (html-escape (or (ensure-v-prefix glojure-version) "unknown")))
                    (format "<tr><td>Go</td><td>%s</td></tr>\n" (html-escape go-ver))
                    (format "<tr><td>OS</td><td>%s</td></tr>\n" (html-escape platform))
                    "</table>\n"
                    (html-source-section sources)
                    (html-section-table sections stripped-size)
                    (html-category-table symbols)
                    (html-package-table symbols top-n)
                    (html-symbol-table symbols top-n)
                    html-sort-script "\n"
                    "<script>hljs.highlightAll();</script>\n"
                    "</body>\n</html>\n")]
    (spit report-path report)
    report-path))

;;------------------------------------------------------------------------------
;; Public API
;;------------------------------------------------------------------------------

(defn- md-source-section [sources]
  (when (seq sources)
    (str "## Source Files\n\n"
         (apply str
                (for [{:keys [name content]} sources
                      :let [ext (last (str/split name #"\."))
                            lang (if (= ext "ys") "yaml" ext)]]
                  (str "<details>\n"
                       "<summary>" name "</summary>\n\n"
                       "```" lang "\n"
                       content
                       (when-not (str/ends-with? content "\n") "\n")
                       "```\n"
                       "</details>\n\n"))))))

(defn generate-report
  "Analyze a binary and write a report (markdown or HTML).
   stripped-binary: path to stripped binary (for file size and sections)
   unstripped-binary: path to unstripped binary (for symbol analysis)
   go-bin: path to go binary
   report-path: where to write the report
   report-format: :html or :md (default :md)
   sources: seq of {:name :content} maps for source files
   top-n: number of top packages/symbols to show (default 30)"
  [{:keys [report-format] :or {report-format :md} :as opts}]
  (if (= report-format :html)
    (generate-html-report opts)
    (let [{:keys [stripped-binary unstripped-binary go-bin report-path
                   output-name goos goarch gloat-version glojure-version
                   pruned sources top-n]
           :or {top-n 30}} opts
          go-version-raw (str/trim (:out (process/shell {:out :string :err :string}
                                                        go-bin "version")))
          go-ver (go-version-short go-version-raw)
          platform (platform-str goos goarch go-version-raw)
          stripped-name (or output-name (fs/file-name stripped-binary))
          unstripped-name (str stripped-name "-unstripped")
          stripped-size (fs/size stripped-binary)
          unstripped-size (fs/size unstripped-binary)
          sections (parse-readelf-sections stripped-binary)
          symbols (parse-nm-symbols go-bin unstripped-binary)
          report (str "# Binary Size Analysis\n\n"
                      "| | |\n"
                      "|---|---|\n"
                      (format "| `%s` | %s |\n" stripped-name (fmt-size stripped-size))
                      (format "| `%s` | %s |\n" unstripped-name (fmt-size unstripped-size))
                      (format "| `-Xprune` | %s |\n" (if pruned "true" "false"))
                      (format "| Gloat | %s |\n" (or (ensure-v-prefix gloat-version) "unknown"))
                      (format "| Glojure | %s |\n" (or (ensure-v-prefix glojure-version) "unknown"))
                      (format "| Go | %s |\n" go-ver)
                      (format "| OS | %s |\n" platform)
                      "\n"
                      (md-source-section sources)
                      (md-section-table sections stripped-size)
                      "\n"
                      (md-category-table symbols)
                      "\n"
                      (md-package-table symbols top-n)
                      "\n"
                      (md-symbol-table symbols top-n))]
      (spit report-path report)
      report-path)))
