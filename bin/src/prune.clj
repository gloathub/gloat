;; prune.clj - Deep dependency-graph prune for gloat
;;
;; Traces the actual dependency graph from the user's -main function,
;; through only the ys runtime functions it actually calls, down to
;; the clojure.core functions those need.  Everything not reachable
;; gets pruned.

(ns prune
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.java.io :as io]))

;;------------------------------------------------------------------------------
;; Go name decoding
;;------------------------------------------------------------------------------

(def GO-DECODE
  {"_DASH_"  "-"
   "_QMARK_" "?"
   "_BANG_"   "!"
   "_STAR_"   "*"
   "_PLUS_"   "+"
   "_GT_"     ">"
   "_LT_"     "<"
   "_EQ_"     "="
   "_SLASH_"  "/"
   "_DOT_"    "."
   "_TICK_"   "'"
   "_AMP_"    "&"
   "_PCT_"    "%"
   "_COLON_"  ":"})

(defn decode-go-name
  "Decode a Go-encoded Clojure name (e.g. 'say' or 'map_DASH_parse')."
  [encoded]
  (reduce (fn [s [pat repl]]
            (str/replace s pat repl))
          encoded
          GO-DECODE))

;;------------------------------------------------------------------------------
;; Loader parsing
;;------------------------------------------------------------------------------

(defn parse-loader
  "Parse a loader.go into structured sections.
   Returns map with:
     :header-lines  - lines before func LoadNS()
     :loadns-open   - the 'func LoadNS() {' line
     :sym-lines     - vec of [line-text var-name]
     :kw-lines      - vec of [line-text var-name]
     :var-lines     - vec of [line-text var-name comment-line]
     :preamble-lines - lines between declarations and function blocks
     :blocks        - vec of [name block-text]
     :closing       - closing brace"
  [loader-path]
  (let [lines (str/split-lines (slurp loader-path))
        n (count lines)
        result (atom {:header-lines []
                      :loadns-open ""
                      :sym-lines []
                      :kw-lines []
                      :var-lines []
                      :preamble-lines []
                      :blocks []
                      :closing "}\n"})
        i (atom 0)]

    ;; Phase 1: Header (before func LoadNS)
    (while (< @i n)
      (let [line (nth lines @i)]
        (if (str/starts-with? line "func LoadNS()")
          (do
            (swap! result assoc :loadns-open (str line "\n"))
            (swap! i inc)
            (while false nil))  ;; break
          (do
            (swap! result update :header-lines conj (str line "\n"))
            (swap! i inc))))
      (when (seq (:loadns-open @result))
        (while false nil)))

    ;; Re-check: if we didn't find LoadNS in phase 1 loop, scan again
    ;; Actually the above loop will stop, let me rewrite properly
    ;; using a loop/recur approach
    (let [lines-with-nl (mapv #(str % "\n") lines)]
      (reset! result {:header-lines []
                      :loadns-open ""
                      :sym-lines []
                      :kw-lines []
                      :var-lines []
                      :preamble-lines []
                      :blocks []
                      :closing "}\n"})
      (reset! i 0)

      ;; Phase 1: Header
      (loop []
        (when (< @i n)
          (let [line (nth lines @i)]
            (if (str/starts-with? line "func LoadNS()")
              (do
                (swap! result assoc :loadns-open (nth lines-with-nl @i))
                (swap! i inc))
              (do
                (swap! result update :header-lines conj (nth lines-with-nl @i))
                (swap! i inc)
                (recur))))))

      ;; Phase 2: sym_, kw_, var_ declarations
      (loop []
        (when (< @i n)
          (let [line (nth lines @i)]
            (cond
              ;; sym_ declaration
              (re-find #"^\t(sym_\w+)\s*:=\s*lang\.NewSymbol\(" line)
              (let [m (re-find #"^\t(sym_\w+)\s*:=\s*lang\.NewSymbol\(" line)]
                (swap! result update :sym-lines conj
                       [(nth lines-with-nl @i) (second m)])
                (swap! i inc)
                (recur))

              ;; kw_ declaration
              (re-find #"^\t(kw_\w+)\s*:=\s*lang\.NewKeyword\(" line)
              (let [m (re-find #"^\t(kw_\w+)\s*:=\s*lang\.NewKeyword\(" line)]
                (swap! result update :kw-lines conj
                       [(nth lines-with-nl @i) (second m)])
                (swap! i inc)
                (recur))

              ;; var_ declaration with preceding // var comment
              (re-find #"^\t// var " line)
              (let [comment-line (nth lines-with-nl @i)]
                (swap! i inc)
                (if (and (< @i n)
                         (re-find #"^\t(var_\w+)\s*:=\s*lang\.InternVarName\("
                                  (nth lines @i)))
                  (let [m (re-find
                           #"^\t(var_\w+)\s*:=\s*lang\.InternVarName\("
                           (nth lines @i))]
                    (swap! result update :var-lines conj
                           [(nth lines-with-nl @i) (second m) comment-line])
                    (swap! i inc)
                    (recur))
                  (do
                    (swap! result update :preamble-lines conj comment-line)
                    (recur))))

              ;; Not a declaration - reached preamble
              :else nil))))

      ;; Phase 3: Preamble (between declarations and function blocks)
      (loop []
        (when (< @i n)
          (let [line (nth lines @i)]
            (if (and (re-find #"^\t// (\S+)$" line)
                     (< (inc @i) n)
                     (re-find #"^\t\{" (nth lines (inc @i))))
              nil ;; function block found, stop preamble
              (do
                (swap! result update :preamble-lines conj
                       (nth lines-with-nl @i))
                (swap! i inc)
                (recur))))))

      ;; Phase 4: Function blocks
      (loop []
        (when (< @i n)
          (let [line (nth lines @i)]
            (cond
              ;; Closing brace of LoadNS
              (or (= line "}") (= line "}\n"))
              (swap! result assoc :closing (str line
                                                (when-not (str/ends-with? line "\n")
                                                  "\n")))

              ;; Function block marker
              (and (re-find #"^\t// (\S+)$" line)
                   (< (inc @i) n)
                   (re-find #"^\t\{" (nth lines (inc @i))))
              (let [m (re-find #"^\t// (\S+)$" line)
                    block-name (second m)
                    block-lines (atom [(nth lines-with-nl @i)])]
                (swap! i inc)
                ;; Track brace depth
                (loop [depth 0]
                  (when (< @i n)
                    (let [bline (nth lines @i)
                          new-depth (+ depth
                                       (count (re-seq #"\{" bline))
                                       (- (count (re-seq #"\}" bline))))]
                      (swap! block-lines conj (nth lines-with-nl @i))
                      (swap! i inc)
                      (when (pos? new-depth)
                        (recur new-depth)))))
                (swap! result update :blocks conj
                       [block-name (apply str @block-lines)])
                (recur))

              ;; Non-block line
              :else
              (do
                (swap! result update :preamble-lines conj
                       (nth lines-with-nl @i))
                (swap! i inc)
                (recur))))))

      @result)))

;;------------------------------------------------------------------------------
;; Reference scanning
;;------------------------------------------------------------------------------

(defn find-used-identifiers
  "Find all sym_, kw_, var_, closed identifiers used in code text."
  [code-text]
  {:syms   (set (map second (re-seq #"\b(sym_\w+)\b" code-text)))
   :kws    (set (map second (re-seq #"\b(kw_\w+)\b" code-text)))
   :vars   (set (map second (re-seq #"\b(var_\w+)\b" code-text)))
   :closed (set (map second (re-seq #"\b(closed\d+)\b" code-text)))})

(defn decode-var-ref
  "Decode a var_ Go identifier to [namespace fn-name].
   e.g. 'var_clojure_DOT_core_apply' -> ['clojure.core' 'apply']
        'var_ys_DOT_v0_say' -> ['ys.v0' 'say']
        'var_ys_DOT_std_map_DASH_parse' -> ['ys.std' 'map-parse']"
  [var-id]
  (when-let [m (re-find #"^var_(.+)$" var-id)]
    (let [encoded (second m)
          ;; Split at _DOT_ boundaries to find namespace parts
          ;; The namespace is everything up to the last _DOT_ segment,
          ;; but we need to figure out where ns ends and name begins.
          ;; Strategy: try known namespace prefixes
          decoded (decode-go-name encoded)]
      ;; The decoded form has dots for _DOT_ and other chars decoded.
      ;; We need to find where the namespace ends.
      ;; Known namespace patterns: clojure.core, clojure.string,
      ;; ys.v0, ys.std, ys.dwim, ys.fs, ys.http, ys.ipc, ys.json,
      ;; yamlscript.common, yamlscript.util
      ;; Try longest match first
      (let [prefixes ["clojure.core" "clojure.string" "clojure.set"
                       "clojure.walk" "clojure.zip" "clojure.test"
                       "yamlscript.common" "yamlscript.util"
                       "ys.v0" "ys.std" "ys.dwim" "ys.fs"
                       "ys.http" "ys.ipc" "ys.json"]]
        (some (fn [prefix]
                (let [prefix-encoded (str/replace
                                      (str/replace prefix "." "_DOT_")
                                      "-" "_DASH_")
                      pattern (str prefix-encoded "_")]
                  (when (str/starts-with? encoded pattern)
                    (let [name-part (subs encoded (count pattern))]
                      [prefix (decode-go-name name-part)]))))
              ;; Sort by length descending to match longest prefix first
              (sort-by count > prefixes))))))

(defn parse-closed-vars
  "Parse the closed variable assignments from preamble text.
   Returns map of closedN -> [namespace fn-name]."
  [preamble-text]
  (let [pattern #"(closed\d+)\s*=\s*lang\.FindOrCreateNamespace\((sym_\w+)\)\.FindInternedVar\((sym_\w+)\)"
        matches (re-seq pattern preamble-text)]
    (into {}
          (for [[_ closed-var ns-sym fn-sym] matches]
            (let [;; Decode the symbol names from the sym_ identifiers
                  ;; sym_ys_DOT_std -> "ys.std"
                  ;; sym_say -> "say"
                  ns-encoded (str/replace ns-sym #"^sym_" "")
                  fn-encoded (str/replace fn-sym #"^sym_" "")
                  ns-name (decode-go-name ns-encoded)
                  fn-name (decode-go-name fn-encoded)]
              [closed-var [ns-name fn-name]])))))

(defn scan-block-refs
  "Extract var_ references from a function block and decode to
   [ns fn-name] pairs. Also resolves closedN references using
   the closed-var-map."
  [block-text closed-var-map]
  (let [{:keys [vars closed]} (find-used-identifiers block-text)
        ;; Decode var_ references
        var-refs (keep decode-var-ref vars)
        ;; Resolve closed variable references
        closed-refs (keep #(get closed-var-map %) closed)]
    (into (set var-refs) closed-refs)))

;;------------------------------------------------------------------------------
;; Namespace location resolution
;;------------------------------------------------------------------------------

(defn ns-to-loader-path
  "Given a namespace and config, return the loader.go path.
   Config keys:
     :build-dir   - the build output directory
     :gloat-root  - gloat project root
     :stdlib-dir  - glojure stdlib directory"
  [ns-name {:keys [build-dir gloat-root stdlib-dir]}]
  (cond
    ;; User code
    (not (or (str/starts-with? ns-name "ys.")
             (str/starts-with? ns-name "yamlscript.")
             (str/starts-with? ns-name "clojure.")))
    (let [ns-path (str/replace ns-name "." "/")]
      (str build-dir "/pkg/" ns-path "/loader.go"))

    ;; ys.* namespaces
    (str/starts-with? ns-name "ys.")
    (let [sub (subs ns-name 3)]
      (str gloat-root "/ys/pkg/ys/" sub "/loader.go"))

    ;; yamlscript.* namespaces
    (str/starts-with? ns-name "yamlscript.")
    (let [sub (subs ns-name 11)]
      (str gloat-root "/ys/pkg/yamlscript/" sub "/loader.go"))

    ;; clojure.core and other clojure.* namespaces
    (str/starts-with? ns-name "clojure.")
    (let [ns-path (str/replace ns-name "." "/")]
      (str stdlib-dir "/" ns-path "/loader.go"))

    :else nil))

;;------------------------------------------------------------------------------
;; Graph building
;;------------------------------------------------------------------------------

(defn build-graph
  "Build the full dependency graph starting from user code.

   Returns:
     {:edges     {source-fn -> #{[ns fn-name] ...}}
      :keeps     {ns-name -> #{fn-name ...}}
      :reasons   {[ns fn-name] -> [ns fn-name]}  ; why each fn is kept}

   Config: {:build-dir :gloat-root :stdlib-dir}"
  [config]
  (let [build-dir (:build-dir config)
        ;; Find user's loader.go files
        user-pkg-dir (str build-dir "/pkg")
        user-loaders (when (fs/exists? user-pkg-dir)
                       (mapv str (fs/glob user-pkg-dir "**/loader.go")))

        ;; Parse user namespace from first loader
        user-ns (when (seq user-loaders)
                  (let [content (slurp (first user-loaders))
                        m (re-find #"RegisterNSLoader\(\"([^\"]+)\"" content)]
                    (when m
                      (str/replace (second m) "/" "."))))

        ;; State
        edges (atom {})       ;; {[ns fn] -> #{[ns fn] ...}}
        keeps (atom {})       ;; {ns -> #{fn ...}}
        reasons (atom {})     ;; {[ns fn] -> [ns fn]}
        visited (atom #{})    ;; set of [ns fn]
        worklist (atom [])    ;; [[ns fn] ...]

        ;; Cache parsed loaders and closed-var-maps
        loader-cache (atom {})  ;; {path -> parsed}
        closed-cache (atom {})  ;; {path -> closed-var-map}

        get-parsed (fn [ns-name]
                     (let [path (ns-to-loader-path ns-name config)]
                       (when (and path (fs/exists? path))
                         (if-let [cached (get @loader-cache path)]
                           cached
                           (let [parsed (parse-loader path)]
                             (swap! loader-cache assoc path parsed)
                             parsed)))))

        get-closed-map (fn [ns-name]
                         (let [path (ns-to-loader-path ns-name config)]
                           (when path
                             (if-let [cached (get @closed-cache path)]
                               cached
                               (when-let [parsed (get-parsed ns-name)]
                                 (let [preamble-text (apply str
                                                            (:preamble-lines
                                                             parsed))
                                       cmap (parse-closed-vars preamble-text)]
                                   (swap! closed-cache assoc path cmap)
                                   cmap))))))

        find-block (fn [ns-name fn-name]
                     (when-let [parsed (get-parsed ns-name)]
                       (some (fn [[name text]]
                               (when (= name fn-name) text))
                             (:blocks parsed))))

        add-keep (fn [ns-name fn-name]
                   (swap! keeps update ns-name (fnil conj #{}) fn-name))]

    ;; Step 1: Seed from user code - scan all user loader blocks
    (doseq [loader-path user-loaders]
      (let [content (slurp loader-path)
            parsed (parse-loader loader-path)
            preamble-text (apply str (:preamble-lines parsed))
            closed-map (parse-closed-vars preamble-text)
            ;; Get the user's namespace from this loader
            ns-match (re-find #"RegisterNSLoader\(\"([^\"]+)\"" content)
            this-ns (when ns-match
                      (str/replace (second ns-match) "/" "."))
            user-fn-key (when this-ns [this-ns "-main"])]

        ;; Cache this parsed loader
        (let [path (str loader-path)]
          (swap! loader-cache assoc path parsed)
          (swap! closed-cache assoc path closed-map))

        ;; Detect namespace alias dependencies (e.g. require '[ys.fs :as fs]).
        ;; These compile to ns.AddAlias(sym_alias, FindOrCreateNamespace(...))
        ;; and the user may call any function dynamically, so we pull in ALL
        ;; functions from aliased namespaces.
        ;; Note: (:require [ys.v0 :refer :all]) is handled differently — it
        ;; generates individual Refer() calls traced by normal var_ scanning.
        (let [alias-matches (re-seq
                             #"ns\.AddAlias\(\w+,\s*lang\.FindOrCreateNamespace\((sym_\w+)\)\)"
                             content)
              aliased-nses (set
                            (for [[_ sym-id] alias-matches
                                  :let [encoded (str/replace sym-id
                                                             #"^sym_" "")
                                        ns-name (decode-go-name encoded)]
                                  :when (and
                                         (not= ns-name this-ns)
                                         (or (str/starts-with? ns-name "ys.")
                                             (str/starts-with?
                                              ns-name "yamlscript.")))]
                              ns-name))]
          ;; For each aliased namespace, add ALL its functions
          (doseq [req-ns aliased-nses]
            (when-let [parsed-ns (get-parsed req-ns)]
              (doseq [[block-name _] (:blocks parsed-ns)]
                (let [ref [req-ns block-name]]
                  (when-not (@visited ref)
                    (swap! worklist conj ref)
                    (swap! visited conj ref)
                    (when user-fn-key
                      (swap! reasons assoc ref user-fn-key))
                    (add-keep req-ns block-name))))))

          ;; Add namespaces detected from CLJ source require forms
          ;; (catches bare `(require 'ys.fs)` that don't appear in loader.go)
          (when-let [src-nses (seq (:source-required-nses config))]
            (doseq [req-ns src-nses]
              (when (and (or (str/starts-with? req-ns "ys.")
                            (str/starts-with? req-ns "yamlscript."))
                        (not (contains? aliased-nses req-ns)))
                (when-let [parsed-ns (get-parsed req-ns)]
                  (doseq [[block-name _] (:blocks parsed-ns)]
                    (let [ref [req-ns block-name]]
                      (when-not (@visited ref)
                        (swap! worklist conj ref)
                        (swap! visited conj ref)
                        (when user-fn-key
                          (swap! reasons assoc ref user-fn-key))
                        (add-keep req-ns block-name)))))))))

        ;; Scan all function blocks in user code
        (doseq [[block-name block-text] (:blocks parsed)]
          (let [refs (scan-block-refs block-text closed-map)
                source-key (when this-ns [this-ns block-name])]
            (when source-key
              ;; Record edges from this user function
              (swap! edges assoc source-key refs)
              ;; Add all refs to worklist
              (doseq [[ref-ns ref-fn :as ref] refs]
                (when-not (@visited ref)
                  (swap! worklist conj ref)
                  (swap! visited conj ref)
                  (when source-key
                    (swap! reasons assoc ref source-key))
                  (add-keep ref-ns ref-fn))))))))

    ;; Step 2: Expand - BFS through ALL namespaces including clojure.core
    (loop []
      (when (seq @worklist)
        (let [items @worklist]
          (reset! worklist [])
          (doseq [[ns-name fn-name :as current] items]
            (when-let [block-text (find-block ns-name fn-name)]
              (let [closed-map (or (get-closed-map ns-name) {})
                    refs (scan-block-refs block-text closed-map)]
                ;; Record edges
                (swap! edges assoc current refs)
                ;; Process refs
                (doseq [[ref-ns ref-fn :as ref] refs]
                  (add-keep ref-ns ref-fn)
                  (when-not (@visited ref)
                    (swap! visited conj ref)
                    (swap! worklist conj ref)
                    (swap! reasons assoc ref current))))))
          (recur))))

    {:edges @edges
     :keeps @keeps
     :reasons @reasons}))

;;------------------------------------------------------------------------------
;; Graph output
;;------------------------------------------------------------------------------

(defn emit-graph-full
  "Emit full trace YAML."
  [graph-result]
  (let [{:keys [edges reasons]} graph-result
        sb (StringBuilder.)]
    ;; Sort entries by key
    (doseq [[[ns fn-name] refs] (sort-by (comp str first) edges)]
      (.append sb (str ns "/" fn-name ":\n"))
      ;; Show reason if not user code
      (when-let [reason (get reasons [ns fn-name])]
        (.append sb (str "  reason: " (first reason) "/" (second reason) "\n")))
      ;; Group refs by namespace
      (let [by-ns (group-by first refs)]
        (doseq [[ref-ns ref-fns] (sort-by first by-ns)]
          (.append sb (str "  " ref-ns ":\n"))
          (doseq [fn-name (sort (map second ref-fns))]
            (.append sb (str "  - " fn-name "\n")))))
      (.append sb "\n"))
    (str sb)))

(defn emit-graph-flat
  "Emit flat keep-lists YAML."
  [graph-result]
  (let [{:keys [keeps]} graph-result
        sb (StringBuilder.)]
    (doseq [[ns-name fns] (sort-by first keeps)]
      (when (seq fns)
        (.append sb (str ns-name ":\n"))
        (doseq [fn-name (sort fns)]
          (.append sb (str "- " fn-name "\n")))
        (.append sb "\n")))
    (str sb)))

;;------------------------------------------------------------------------------
;; Loader pruning (reused from prune-core.py logic)
;;------------------------------------------------------------------------------

(defn prune-closed-vars
  "Remove unused closed variable blocks from preamble text."
  [preamble-text used-closed]
  (let [lines (str/split-lines preamble-text)
        n (count lines)]
    (loop [i 0
           result []]
      (if (>= i n)
        (str (str/join "\n" result)
             (when (seq result) "\n"))
        (let [line (nth lines i)
              closed-match (re-find #"^\tvar (closed\d+) any" line)]
          (if (and closed-match
                   (not (contains? used-closed (second closed-match))))
            ;; Skip this declaration and its block
            (let [next-i (inc i)
                  skip-to (if (and (< next-i n)
                                   (= (str/trim (nth lines next-i)) "{"))
                            ;; Skip past the { ... } block
                            (loop [j (inc next-i)
                                   depth 1]
                              (if (or (>= j n) (<= depth 0))
                                j
                                (let [l (nth lines j)
                                      nd (+ depth
                                            (count (re-seq #"\{" l))
                                            (- (count (re-seq #"\}" l))))]
                                  (recur (inc j) (if (<= nd 0) 0 nd)))))
                            next-i)]
              (recur skip-to result))
            (recur (inc i) (conj result line))))))))

(defn find-used-imports
  "Determine which import aliases are actually used in the code."
  [full-code]
  (let [import-pattern #"(?m)^\t(\w+)\s+\"[^\"]+\"\s*$"
        matches (re-seq import-pattern full-code)]
    (set
     (for [[_ alias] matches
           :when (re-find (re-pattern (str "\\b" (java.util.regex.Pattern/quote alias) "\\b\\."))
                          full-code)]
       alias))))

(defn prune-imports
  "Remove unused imports from header lines."
  [header-lines used-imports]
  (let [always-keep #{"lang" "runtime" "fmt"}]
    (loop [lines header-lines
           result []
           in-import false
           depth 0]
      (if (empty? lines)
        result
        (let [line (first lines)
              rest-lines (rest lines)]
          (cond
            (str/includes? line "import (")
            (recur rest-lines (conj result line)
                   true 1)

            in-import
            (let [new-depth (+ depth
                               (count (re-seq #"\(" line))
                               (- (count (re-seq #"\)" line))))]
              (if (<= new-depth 0)
                (recur rest-lines (conj result line) false 0)
                (let [import-match (re-find #"^\t(\w+)\s+\"" line)]
                  (if import-match
                    (let [alias (second import-match)]
                      (if (or (contains? always-keep alias)
                              (contains? used-imports alias))
                        (recur rest-lines (conj result line) true new-depth)
                        (recur rest-lines result true new-depth)))
                    (recur rest-lines (conj result line) true new-depth)))))

            :else
            (recur rest-lines (conj result line) false depth)))))))

(defn generate-pruned-loader
  "Generate pruned loader.go content from a parsed loader and keep-set."
  [parsed keep-set]
  (let [;; Step 1: Filter function blocks
        kept-blocks (filterv (fn [[name _]] (contains? keep-set name))
                             (:blocks parsed))

        ;; Step 2: Prune closed variables from preamble
        all-kept-code (apply str (map second kept-blocks))
        preamble-text (apply str (:preamble-lines parsed))
        {:keys [closed]} (find-used-identifiers all-kept-code)
        pruned-preamble (prune-closed-vars preamble-text closed)

        ;; Step 3: Find used var_ identifiers
        code-for-var-scan (str pruned-preamble all-kept-code)
        used-vars (:vars (find-used-identifiers code-for-var-scan))

        ;; Step 4: Filter var declarations
        kept-var-lines (filterv (fn [[_ var-name _]]
                                  (contains? used-vars var-name))
                                (:var-lines parsed))

        ;; Step 5: Scan for used sym_ and kw_ identifiers
        kept-var-text (str (apply str (map first kept-var-lines))
                           (apply str (map #(nth % 2) kept-var-lines)))
        all-scannable (str code-for-var-scan kept-var-text)
        {:keys [syms kws]} (find-used-identifiers all-scannable)

        ;; Step 6-7: Filter sym and kw declarations
        kept-syms (filterv (fn [[_ var-name]] (contains? syms var-name))
                           (:sym-lines parsed))
        kept-kws (filterv (fn [[_ var-name]] (contains? kws var-name))
                          (:kw-lines parsed))

        ;; Step 8: Format kept var declarations
        kept-vars-text (apply str
                              (mapcat (fn [[line _ comment]]
                                        [comment line])
                                      kept-var-lines))

        ;; Step 9: Assemble
        blocks-text (apply str (map second kept-blocks))
        header-text (apply str (:header-lines parsed))
        body-code (str (apply str (map first kept-syms))
                       (apply str (map first kept-kws))
                       kept-vars-text
                       pruned-preamble
                       blocks-text)
        full-text (str header-text (:loadns-open parsed) body-code
                       (:closing parsed))

        ;; Step 10: Prune imports
        used-imports (find-used-imports full-text)
        pruned-header (prune-imports (:header-lines parsed) used-imports)

        ;; Step 11: Final assembly
        output (str (apply str pruned-header)
                    (:loadns-open parsed)
                    (apply str (map first kept-syms))
                    (apply str (map first kept-kws))
                    kept-vars-text
                    pruned-preamble
                    blocks-text
                    (:closing parsed))]

    {:output output
     :kept (count kept-blocks)
     :total (count (:blocks parsed))}))

;;------------------------------------------------------------------------------
;; Orchestration
;;------------------------------------------------------------------------------

(defn load-core-graph
  "Load the clojure-core.yaml dependency graph."
  [graph-path]
  (let [content (slurp graph-path)
        ;; Simple YAML parser for this specific format:
        ;; key:\n- dep1\n- dep2\n
        lines (str/split-lines content)]
    (loop [lines lines
           graph {}
           current-key nil
           current-deps []]
      (if (empty? lines)
        (if current-key
          (assoc graph current-key current-deps)
          graph)
        (let [line (first lines)
              rest-lines (rest lines)]
          (cond
            ;; New key (may be quoted)
            (re-find #"^'?[^-\s]" line)
            (let [new-graph (if current-key
                              (assoc graph current-key current-deps)
                              graph)
                  ;; Parse key - handle both 'key': and key: forms
                  key (-> line
                          (str/replace #":$" "")
                          (str/replace #"^'" "")
                          (str/replace #"'$" ""))]
              (recur rest-lines new-graph key []))

            ;; Dependency item
            (re-find #"^- " line)
            (let [dep (-> line
                          (str/replace #"^- " "")
                          str/trim
                          (str/replace #"^'" "")
                          (str/replace #"'$" ""))]
              (recur rest-lines graph current-key (conj current-deps dep)))

            ;; Empty line or other
            :else
            (recur rest-lines graph current-key current-deps)))))))

(defn prune-all
  "Orchestrate: build graph, prune each layer, write pruned loaders.

   Config keys:
     :build-dir     - build output directory
     :gloat-root    - gloat project root
     :stdlib-dir    - glojure stdlib directory
     :core-graph-path - path to clojure-core.yaml
     :runtime-keeps - extra clojure.core functions to always keep
     :graph-mode    - nil, :full, or :flat
     :quiet         - suppress messages
     :verbose       - verbose timing

   Returns:
     {:graph-result  - the dependency graph
      :used-namespaces - set of ys/yamlscript namespaces that are needed
      :stats         - map of ns -> {:kept N :total N}}"
  [config]
  (let [{:keys [build-dir gloat-root stdlib-dir core-graph-path
                runtime-keeps graph-mode quiet verbose]} config

        ;; Build the full dependency graph (now scans clojure.core
        ;; loader.go blocks directly instead of using clojure-core.yaml)
        graph-config {:build-dir build-dir
                      :gloat-root gloat-root
                      :stdlib-dir stdlib-dir
                      :source-required-nses
                      (:source-required-nses config)}
        graph-result (build-graph graph-config)

        ;; Scan always-imported Glojure stdlib packages for clojure.core deps.
        ;; These packages are NOT pruned (imported from Glojure module), so
        ;; all their blocks are present and may call any clojure.core function.
        stdlib-core-deps
        (let [always-imported ["glojure/go/io"
                               "clojure/core/async"
                               "clojure/core/protocols"
                               "clojure/string"]
              deps (atom #{})]
          (doseq [pkg-path always-imported]
            (let [loader (str stdlib-dir "/" pkg-path "/loader.go")]
              (when (fs/exists? loader)
                (let [content (slurp loader)
                      var-refs (re-seq #"\bvar_clojure_DOT_core_(\w+)\b"
                                       content)]
                  (doseq [[_ encoded-name] var-refs]
                    (swap! deps conj (decode-go-name encoded-name)))))))
          @deps)

        ;; Add runtime keeps AND stdlib deps, expand through the graph
        runtime-set (into (set runtime-keeps) stdlib-core-deps)
        graph-result (if (seq runtime-set)
                       ;; Re-run BFS from runtime keeps + stdlib deps
                       (let [visited (atom (set (for [[ns fns] (:keeps graph-result)
                                                      fn-name fns]
                                                  [ns fn-name])))
                             keeps (atom (:keeps graph-result))
                             edges (atom (:edges graph-result))
                             reasons (atom (:reasons graph-result))
                             worklist (atom (vec (for [fn-name runtime-set]
                                                  ["clojure.core" fn-name])))

                             loader-cache (atom {})
                             closed-cache (atom {})

                             get-parsed (fn [ns-name]
                                          (let [path (ns-to-loader-path
                                                      ns-name graph-config)]
                                            (when (and path (fs/exists? path))
                                              (if-let [cached (get @loader-cache
                                                                   path)]
                                                cached
                                                (let [parsed (parse-loader path)]
                                                  (swap! loader-cache assoc
                                                         path parsed)
                                                  parsed)))))

                             get-closed-map (fn [ns-name]
                                              (let [path (ns-to-loader-path
                                                          ns-name graph-config)]
                                                (when path
                                                  (if-let [cached
                                                           (get @closed-cache
                                                                path)]
                                                    cached
                                                    (when-let [parsed
                                                               (get-parsed
                                                                ns-name)]
                                                      (let [pt (apply str
                                                                      (:preamble-lines
                                                                       parsed))
                                                            cmap (parse-closed-vars
                                                                  pt)]
                                                        (swap! closed-cache assoc
                                                               path cmap)
                                                        cmap))))))

                             find-block (fn [ns-name fn-name]
                                          (when-let [parsed (get-parsed ns-name)]
                                            (some (fn [[name text]]
                                                    (when (= name fn-name) text))
                                                  (:blocks parsed))))

                             add-keep (fn [ns-name fn-name]
                                        (swap! keeps update ns-name
                                               (fnil conj #{}) fn-name))]

                         ;; Add runtime keeps
                         (doseq [fn-name runtime-set]
                           (add-keep "clojure.core" fn-name))

                         ;; BFS expand
                         (loop []
                           (when (seq @worklist)
                             (let [items @worklist]
                               (reset! worklist [])
                               (doseq [[ns-name fn-name :as current] items]
                                 (when-not (@visited current)
                                   (swap! visited conj current)
                                   (add-keep ns-name fn-name)
                                   (when-let [block-text (find-block
                                                          ns-name fn-name)]
                                     (let [closed-map (or (get-closed-map
                                                           ns-name) {})
                                           refs (scan-block-refs
                                                 block-text closed-map)]
                                       (swap! edges assoc current refs)
                                       (doseq [[ref-ns ref-fn :as ref] refs]
                                         (add-keep ref-ns ref-fn)
                                         (when-not (@visited ref)
                                           (swap! worklist conj ref)
                                           (swap! reasons assoc
                                                  ref current)))))))
                               (recur))))

                         {:edges @edges
                          :keeps @keeps
                          :reasons @reasons})
                       graph-result)

        core-keeps (get-in graph-result [:keeps "clojure.core"] #{})

        ;; Emit graph if requested
        _ (when graph-mode
            (case graph-mode
              :full (print (emit-graph-full graph-result))
              :flat (print (emit-graph-flat graph-result)))
            (flush))

        ;; Determine which ys/yamlscript namespaces are actually used
        used-namespaces (set
                         (for [[ns-name fns] (:keeps graph-result)
                               :when (and (seq fns)
                                          (or (str/starts-with? ns-name "ys.")
                                              (str/starts-with?
                                               ns-name "yamlscript.")))]
                           ns-name))

        ;; Prune clojure.core
        stats (atom {})
        core-loader-path (str stdlib-dir "/clojure/core/loader.go")
        _ (when (fs/exists? core-loader-path)
            (let [parsed (parse-loader core-loader-path)
                  result (generate-pruned-loader parsed core-keeps)
                  output-path (str build-dir
                                   "/internal/stdlib/clojure/core/loader.go")]
              (fs/create-dirs (fs/parent output-path))
              (spit output-path (:output result))
              (swap! stats assoc "clojure.core"
                     {:kept (:kept result) :total (:total result)})
              (when-not quiet
                (binding [*out* *err*]
                  (println (str "Pruned clojure.core: "
                                (:kept result) "/" (:total result)
                                " functions kept ("
                                (- (:total result) (:kept result))
                                " removed)"))))))

        ;; Prune each ys/yamlscript namespace
        _ (doseq [ns-name used-namespaces]
            (let [ns-keeps (get-in graph-result [:keeps ns-name] #{})
                  loader-path (ns-to-loader-path ns-name graph-config)]
              (when (and loader-path (fs/exists? loader-path))
                (let [parsed (parse-loader loader-path)
                      result (generate-pruned-loader parsed ns-keeps)
                      ;; Build internal path
                      ns-path (cond
                                (str/starts-with? ns-name "ys.")
                                (str "ys/" (subs ns-name 3))
                                (str/starts-with? ns-name "yamlscript.")
                                (str "yamlscript/" (subs ns-name 11))
                                :else
                                (str/replace ns-name "." "/"))
                      output-path (str build-dir "/internal/"
                                       ns-path "/loader.go")]
                  (fs/create-dirs (fs/parent output-path))
                  (spit output-path (:output result))
                  (swap! stats assoc ns-name
                         {:kept (:kept result) :total (:total result)})
                  (when-not quiet
                    (binding [*out* *err*]
                      (println (str "Pruned " ns-name ": "
                                    (:kept result) "/" (:total result)
                                    " functions kept ("
                                    (- (:total result) (:kept result))
                                    " removed)"))))))))]

    {:graph-result graph-result
     :used-namespaces used-namespaces
     :stats @stats}))
