#!/usr/bin/env bb

;; gloat - Glojure AOT Tool
;; Compiles YAMLScript/Clojure/Glojure to Go, binaries, or Wasm

(ns gloat
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))


;;------------------------------------------------------------------------------
;; Constants
;;------------------------------------------------------------------------------

(def VERSION (System/getenv "GLOAT_VERSION"))

(def GLOAT-ROOT
  (str (fs/parent (fs/parent (fs/parent (fs/canonicalize *file*))))))

(load-file (str GLOAT-ROOT "/bin/src/prune.clj"))
(load-file (str GLOAT-ROOT "/bin/src/deps.clj"))
(load-file (str GLOAT-ROOT "/bin/src/html.clj"))
(load-file (str GLOAT-ROOT "/bin/src/open.clj"))
(load-file (str GLOAT-ROOT "/bin/src/serve.clj"))

(def TEMPLATE (str GLOAT-ROOT "/template"))
(def SRC (str GLOAT-ROOT "/ys/src"))

(def VALID-EXTENSIONS #{"gzip" "brotli" "prune" "deps" "html" "serve" "open"})

(def go-env
  {"GOPATH"     (str GLOAT-ROOT "/.cache/.local/go")
   "GOMODCACHE" (str GLOAT-ROOT "/.cache/.local/go/pkg/mod")
   "GOCACHE"    (str GLOAT-ROOT "/.cache/.local/cache/go-build")})

;;------------------------------------------------------------------------------
;; Shell Completion Scripts
;;------------------------------------------------------------------------------

(def bash-completion-script (slurp (str TEMPLATE "/completion.bash")))
(def zsh-completion-script (slurp (str TEMPLATE "/completion.zsh")))
(def fish-completion-script (slurp (str TEMPLATE "/completion.fish")))

;;------------------------------------------------------------------------------
;; Dynamic State
;;------------------------------------------------------------------------------

(def ^:dynamic *opts* {})
(def ^:dynamic *source-file* nil)
(def ^:dynamic *compile-start* nil)
(def ^:dynamic *timer-start* nil)

;;------------------------------------------------------------------------------
;; Getopt Spec
;;------------------------------------------------------------------------------

(def getopt-spec (System/getenv "GETOPT_SPEC"))

;;------------------------------------------------------------------------------
;; Helper Functions
;;------------------------------------------------------------------------------

(defn die
  ([msg]
   (binding [*out* *err*]
     (println msg))
   (System/exit 1))
  ([msg & more]
   (die (apply str msg more))))

(defn msg [& args]
  (when-not (:quiet *opts*)
    (apply println args)))

(defn timer-start []
  (when (and (:verbose *opts*) (not (:quiet *opts*)))
    (alter-var-root #'*timer-start* (constantly (System/currentTimeMillis)))))

(defn timer-end [label]
  (when (and (:verbose *opts*) (not (:quiet *opts*)) *timer-start*)
    (let [elapsed (- (System/currentTimeMillis) *timer-start*)]
      (binding [*out* *err*]
        (println (str "  " label "... done (" elapsed "ms)")))))
  (alter-var-root #'*timer-start* (constantly nil)))

(def make-vars nil)

(defn setup []
  (let [result (process/shell
                 {:out :string
                  :dir GLOAT-ROOT
                  :extra-env go-env}
                 "make" "--quiet" "--no-print-directory"
                 "gloat-vars")]
    (alter-var-root #'make-vars
      (constantly (edn/read-string (str/trim (:out result)))))))

;;------------------------------------------------------------------------------
;; Option Parsing
;;------------------------------------------------------------------------------

(defn parse-opts [args]
  (if-let [opts-env (System/getenv "GLOAT_OPTS")]
    (edn/read-string opts-env)
    (let [proc (apply process/process
                      {:in getopt-spec
                       :out :string
                       :err :inherit}
                      (str GLOAT-ROOT "/util/getopt")
                      args)
          result @proc]
      (when-not (zero? (:exit result))
        (System/exit (:exit result)))
      (edn/read-string (:out result)))))

;;------------------------------------------------------------------------------
;; File Type and Format Detection
;;------------------------------------------------------------------------------

(defn get-file-type [file]
  (cond
    (str/ends-with? file ".ys") "ys"
    (str/ends-with? file ".clj") "clj"
    (str/ends-with? file ".glj") "glj"
    (str/ends-with? file ".go") "go"
    (str/ends-with? file ".bb") "bb"
    (str/ends-with? file ".wasm") "wasm"
    (str/ends-with? file ".so") "lib"
    (str/ends-with? file ".dylib") "lib"
    (str/ends-with? file "/") "dir"
    :else
    ;; Check shebang and file content for type detection
    (if (and (fs/exists? file)
             (fs/regular-file? file))
      (try
        (with-open [rdr (io/reader file)]
          (let [first-line (first (line-seq rdr))]
            (cond
              ;; YAMLScript shebang or directive
              (and first-line
                   (or (re-find #"^#!/.*\bys-?\d" first-line)
                       (re-find #"^!yamlscript/" first-line)))
              "ys"

              ;; Babashka/Clojure shebang
              (and first-line
                   (re-find #"^#!/.*\b(bb|clojure|clj)\b" first-line))
              "clj"

              :else "unknown")))
        (catch Exception _ "unknown"))
      "unknown")))

(defn infer-format [output to]
  (cond
    ;; Explicit -t flag takes precedence
    to to

    ;; No output = stdout with go format
    (not output) "go"

    ;; Infer from extension
    (str/ends-with? output ".bb") "bb"
    (str/ends-with? output ".clj") "clj"
    (str/ends-with? output ".glj") "glj"
    (str/ends-with? output ".go") "go"
    (str/ends-with? output ".so") "lib"
    (str/ends-with? output ".dylib") "lib"
    (str/ends-with? output ".wasm") "wasm"
    (str/ends-with? output ".js") "js"
    (str/ends-with? output "/") "dir"

    ;; No extension or unrecognized = binary
    :else "bin"))

;;------------------------------------------------------------------------------
;; Namespace Handling
;;------------------------------------------------------------------------------

(defn derive-namespace [file]
  (let [basename (fs/file-name file)
        name (-> basename
                 (str/replace #"\.[^.]+$" "")
                 (str/replace #"-" "_"))
        name (if (re-find #"^\d" name)
               (str "_" name)
               name)]
    (str name ".core")))

(defn parse-namespace [file]
  (when (fs/exists? file)
    (try
      (with-open [rdr (io/reader (str file))]
        (let [content (slurp rdr)
              match (re-find #"(?m)^\(ns\s+([^\s)]+)" content)]
          (when match (second match))))
      (catch Exception _ nil))))

(defn resolve-namespace [file ns-override]
  (or (when (seq ns-override) ns-override)
      (not-empty (System/getenv "GLOAT_NAMESPACE"))
      (parse-namespace file)
      (derive-namespace file)))

;;------------------------------------------------------------------------------
;; Template Rendering
;;------------------------------------------------------------------------------

(defn render-template [template-str replacements]
  (reduce (fn [s [from to]]
            (str/replace s from (str to)))
          template-str
          replacements))

(defn has-main-fn? [clj-content]
  "Check if Clojure code contains a (defn main ...) definition."
  (boolean (re-find #"\(defn\s+main\b" clj-content)))

(defn extract-export [clj-content]
  "Extract EXPORT map from Clojure code. Returns nil if not found.
  Handles both standard map literals and YAMLScript's (% ...) format."
  (when-let [match (re-find #"(?s)\(def\s+EXPORT\s+(.+?)\)\s*\n" clj-content)]
    (try
      (let [content (str/trim (second match))]
        (if (str/starts-with? content "(%")
          ;; Parse YAMLScript (% "key1" val1 "key2" val2 ...) format
          (let [inner (-> content
                          (str/replace #"^\(%\s*" "")
                          (str/replace #"\s*\)$" ""))
                ;; Read as EDN sequence
                items (edn/read-string (str "[" inner "]"))
                ;; Convert to map (pairs of key, value)
                pairs (partition 2 items)
                ;; Build map with keyword keys and normalized types
                result (into {}
                             (map (fn [[k v]]
                                    (let [key (keyword k)
                                          ;; Convert string types to keywords
                                          val (if (vector? v)
                                                (mapv #(if (nil? %)
                                                         :null
                                                         (keyword %))
                                                      v)
                                                (if (nil? v)
                                                  :null
                                                  (keyword v)))]
                                      [key val]))
                                  pairs))]
            result)
          ;; Try standard EDN map format
          (edn/read-string content)))
      (catch Exception e
        (binding [*out* *err*]
          (println "Warning: Failed to parse EXPORT:" (.getMessage e)))
        nil))))

(defn kebab-to-snake [s]
  "Convert kebab-case to snake_case."
  (str/replace s #"-" "_"))

(def type-mappings
  "Map type keywords to Go cgo type info."
  {:int    {:go-type "C.longlong"
            :go-to-clj "int64(arg)"
            :clj-to-go "C.longlong(result.(int64))"}
   :float  {:go-type "C.double"
            :go-to-clj "float64(arg)"
            :clj-to-go "C.double(result.(float64))"}
   :str    {:go-type "*C.char"
            :go-to-clj "C.GoString(arg)"
            :clj-to-go "C.CString(result.(string))"}
   :bool   {:go-type "C.int"
            :go-to-clj "arg != 0"
            :clj-to-go-special true}
   :null   {:no-return true}})

(defn generate-export-function [fn-name type-spec namespace]
  "Generate a single //export Go function wrapper.
  type-spec is a vector like [arg-types... return-type]."
  (let [type-vec (if (vector? type-spec) type-spec [type-spec])
        return-type (last type-vec)
        arg-types (if (> (count type-vec) 1)
                    (butlast type-vec)
                    [])
        return-info (get type-mappings return-type)
        c-fn-name (kebab-to-snake fn-name)

        ;; Generate parameter list
        params (str/join ", "
                         (map-indexed
                          (fn [idx arg-type]
                            (let [type-info (get type-mappings arg-type)]
                              (str "arg" idx " " (:go-type type-info))))
                          arg-types))

        ;; Generate argument conversions
        arg-conversions (map-indexed
                         (fn [idx arg-type]
                           (let [type-info (get type-mappings arg-type)
                                 conversion (:go-to-clj type-info)]
                             (str/replace conversion "arg" (str "arg" idx))))
                         arg-types)

        ;; Generate function signature
        signature (if (:no-return return-info)
                    (str "func " c-fn-name "(" params ")")
                    (str "func " c-fn-name "(" params ") "
                         (:go-type return-info)))

        ;; Generate function body
        invoke-args (if (seq arg-conversions)
                      (str/join ", " arg-conversions)
                      "")
        invoke-line (if (seq arg-conversions)
                      (str "\tfn.Invoke(" invoke-args ")")
                      "\tfn.Invoke()")

        body (if (:no-return return-info)
               ;; Void return
               (str "\tfn := glj.Var(\"" namespace "\", \"" fn-name "\")\n"
                    invoke-line "\n")
               ;; Has return value
               (str "\tfn := glj.Var(\"" namespace "\", \"" fn-name "\")\n"
                    "\tresult := " invoke-line "\n"
                    (if (:clj-to-go-special return-info)
                      ;; Special handling for bool
                      "\tif result.(bool) {\n\t\treturn 1\n\t}\n\treturn 0\n"
                      ;; Standard type conversion
                      (str "\treturn " (:clj-to-go return-info) "\n"))))]

    (str "//export " c-fn-name "\n"
         signature " {\n"
         body
         "}\n")))

(defn generate-export-functions [export-map namespace]
  "Generate all //export function wrappers from EXPORT map."
  (if (empty? export-map)
    ""
    (str/join "\n"
              (map (fn [[fn-name type-spec]]
                     (generate-export-function
                      (name fn-name) type-spec namespace))
                   export-map))))

;;------------------------------------------------------------------------------
;; Info Commands
;;------------------------------------------------------------------------------

(defn do-version []
  (when (:version *opts*)
    (println (str "gloat version " VERSION))
    (let [glojure-version (:GLOJURE-VERSION make-vars)]
      (when (seq glojure-version)
        (println (str "glojure version " glojure-version))))
    (System/exit 0)))

(defn do-formats []
  (when (:formats *opts*)
    (println "Available output formats (use with -t/--to):

Source formats:
  clj       Clojure source file
  bb        Babashka-ready source file (self-contained)
  glj       Glojure source file
  go        Go source (default for stdout)
  dir       Go project directory

Binary formats:
  bin       Native binary (default when -o has no extension)
  lib       Shared library (.so .dylib .dll)
  wasm      WebAssembly wasip1 target
  js        WebAssembly js target

Format can usually be inferred from -o extension:
  .clj → clj      .glj → glj    .go → go
   .so → lib    .dylib → lib   .dll → lib
   .js → js     .wasm → wasm
     / → dir    <none> → bin   .exe → bin")
    (System/exit 0)))

(defn do-extensions []
  (when (:extensions *opts*)
    (println "Available processing extensions (use with -X/--ext):

  brotli      Compress with brotli (auto-installed if needed)
  deps        Print flat dependency list (implies prune)
  deps=tree   Print dependency tree (implies prune)
  gzip        Compress with gzip (requires gzip command)
  html        Generate HTML page for js/wasm (-Xhtml or -Xhtml='args')
  open        Open browser after serving (-Xopen or -Xopen='args')
  prune       Prune unused clojure.core functions (smaller binaries)
  serve       Start a local HTTP server after building (-Xserve)

The compression extensions are applied to WASM output formats (wasm, js).
The html, serve, and open extensions are only valid with js format (-o foo.js or -t js).
The prune extension applies to binary builds (bin, lib, wasm, js, dir).

Multiple extensions can be combined with commas: -Xserve,html=100
-Xopen implies -Xserve which implies -Xhtml.")
    (System/exit 0)))

(defn do-platforms []
  (when (:platforms *opts*)
    (println
     "Available cross-compilation platforms (use with --platform=OS/ARCH):

Common platforms:
  OS         ARCH
  =======    ======================
  linux      amd64, arm64, 386, arm
  darwin     amd64, arm64
  windows    amd64, arm64, arm, 386

  freebsd    amd64, arm64, 386
  openbsd    amd64, arm64
  netbsd     amd64, arm64

  wasip1     wasm
  js         wasm

Less common:
  linux      ppc64le, s390x, riscv64, mips64le
  dragonfly  amd64
")
    (System/exit 0)))

(defn do-shell []
  (when (:shell *opts*)
    (die "Use 'gloat --shell' from the command line (not via gloat.clj)")))

(defn do-shell-all []
  (when (:shell-all *opts*)
    (die "Use 'gloat --shell-all' from the command line (not via gloat.clj)")))

(defn do-reset []
  (when (:reset *opts*)
    (die "Use 'gloat --reset' from the command line (not via gloat.clj)")))

(defn do-upgrade []
  (when (:upgrade *opts*)
    (die "Use 'gloat --upgrade' from the command line (not via gloat.clj)")))

(defn do-complete []
  (when-let [shell (:complete *opts*)]
    (case shell
      "bash" (print bash-completion-script)
      "zsh"  (print zsh-completion-script)
      "fish" (print fish-completion-script)
      (die "Unknown shell for --complete: " shell
           " (use bash, zsh, or fish)"))
    (flush)
    (System/exit 0)))

(defn parse-extensions
  "Parse ext vector into map. Supports comma-separated values.
   e.g. [\"prune\" \"deps=tree\" \"serve,html=100\"]
     -> {\"prune\" true, \"deps\" \"tree\", \"serve\" true, \"html\" \"100\"}"
  [ext-vec]
  (into {}
        (for [ext (mapcat #(str/split % #",") ext-vec)]
          (if-let [eq-idx (str/index-of ext "=")]
            [(subs ext 0 eq-idx) (subs ext (inc eq-idx))]
            [ext true]))))

(defn validate-extensions []
  (when (seq (:ext *opts*))
    (let [parsed (parse-extensions (:ext *opts*))]
      (doseq [[ext-name ext-val] parsed]
        (when-not (VALID-EXTENSIONS ext-name)
          (die "Unknown extension: " ext-name
               " (see --extensions for available extensions)")))
      ;; Validate deps values
      (when-let [deps-val (get parsed "deps")]
        (when (and (string? deps-val)
                   (not (contains? #{"tree"} deps-val)))
          (die "Unknown deps mode: " deps-val
               " (use tree, list, or tree-sort)")))
      ;; Validate html, serve, and open are only used with js format
      (doseq [ext ["html" "serve" "open"]]
        (when (contains? parsed ext)
          (let [format (infer-format (:out *opts*) (:to *opts*))]
            (when (not= format "js")
              (die (str "-X" ext " is only valid with js format"
                        " (-o foo.js or -t js)")))))))))

;;------------------------------------------------------------------------------
;; Core Conversion Functions
;;------------------------------------------------------------------------------

;; Forward declarations
(declare convert-directory convert-files)

(defn ys-to-clj [input output namespace]
  (let [ys (:YS make-vars)]
    (timer-start)

    ;; Compile YS to Clojure
    (let [result (process/shell
                  {:out :string
                   :extra-env go-env}
                  ys "-c" input)
          body (-> (->> (:out result)
                        str/split-lines
                        (remove #(= % "(apply main ARGS)"))
                        (str/join "\n"))
                   ;; Apply perl-like transformations
                   (str/replace #"\(defn\n (\S+)" "(defn $1")
                   (str/replace #"\(defn (\S+)\n (\[)" "(defn $1 $2")
                   (str/replace #"\)\n\(defn" ")\n\n(defn")
                   (str/replace #"\)\n\(declare" ")\n\n(declare"))]

      (timer-end "YS→CLJ")

      ;; Get source file paths
      (let [source-abs (if *source-file*
                         *source-file*
                         (str (fs/canonicalize input)))
            source-dir (str (fs/parent source-abs))
            ;; Check if body has main function
            main-fn (if (has-main-fn? body)
                      "
(defn -main [& argv]
  (let [args (map-parse argv)]
    (alter-var-root #'ARGV (constantly argv))
    (alter-var-root #'ARGS (constantly args))
    (alter-var-root #'FILE (constantly \"SOURCE-FILE\"))
    (alter-var-root #'DIR (constantly \"SOURCE-DIR\"))
    (apply main args)))
"
                      "")
            template-content (slurp (str TEMPLATE "/clojure.clj"))
            result-content (render-template template-content
                                            [["NAMESPACE" namespace]
                                             ["BODY\n" (str body "\n")]
                                             ["MAIN-FN" main-fn]
                                             ["SOURCE-FILE" source-abs]
                                             ["SOURCE-DIR" source-dir]])]
        (spit output result-content)))))

(defn clj-to-glj [input output]
  (let [bb (:BB make-vars)
        glojure-dir (:GLOJURE-DIR make-vars)
        rewrite-script (str glojure-dir "/scripts/rewrite-core/rewrite.clj")
        name (-> (fs/file-name input) (str/replace #"\.clj$" ""))
        parent (fs/file-name (fs/parent input))
        label (if (or (= parent "ys") (= parent "yamlscript"))
                (str parent "." name)
                name)]
    (timer-start)
    (let [result (process/shell
                  {:out :string
                   :extra-env go-env}
                  bb rewrite-script input)]
      (spit output (:out result)))
    (timer-end (str "CLJ→GLJ (" label ")"))))

(defn glj-to-go [input namespace output-dir]
  (let [glj (:GLJ make-vars)
        ns-path (-> namespace
                    (str/replace #"\." "/")
                    (str/replace #"-" "_"))
        ns-dir (if (str/includes? ns-path "/")
                 (subs ns-path 0 (str/last-index-of ns-path "/"))
                 "")
        ns-file (str (last (str/split namespace #"\.")) ".glj")]

    (timer-start)

    ;; Create namespace directory structure
    (fs/create-dirs (str output-dir "/" ns-dir))

    ;; Copy input to namespace structure
    (fs/copy input (str output-dir "/" ns-path ".glj") {:replace-existing true})

    ;; Copy pre-compiled ys runtime and dependencies
    (let [ys-glj-dir (str GLOAT-ROOT "/ys/glj")]
      (doseq [file (fs/glob ys-glj-dir "**/*")]
        (when (fs/regular-file? file)
          (let [rel-path (str (fs/relativize ys-glj-dir file))
                target (str output-dir "/" rel-path)]
            (fs/create-dirs (fs/parent target))
            (fs/copy file target {:replace-existing true})))))

    ;; Compile user namespace only
    (let [compile-cmd (str "(compile (quote " namespace "))")
          opts {:in compile-cmd
                :dir output-dir
                :extra-env go-env}
          opts (if (:quiet *opts*)
                 (assoc opts :out :string :err :string)
                 opts)]
      (try
        (process/shell opts glj)
        (catch Exception _ nil)))

    (timer-end "GLJ→GO")))

(defn compress-wasm [file exts]
  (doseq [ext exts]
    (case ext
      "gzip"
      (do
        (when-not (fs/which "gzip")
          (die "gzip not found (required by -Xgzip)"))
        (process/shell "gzip" "-9" "-f" file)
        (fs/move (str file ".gz") file {:replace-existing true}))

      "brotli"
      (let [brotli-bin (:BROTLI make-vars)]
        (when-not (fs/exists? brotli-bin)
          (process/shell
           {:dir GLOAT-ROOT
            :extra-env go-env
            :out :inherit
            :err :inherit}
           "make" "--quiet" "--no-print-directory" brotli-bin))
        (process/shell brotli-bin "-9" "-f" file)
        (fs/move (str file ".br") file {:replace-existing true})))))


(defn find-glojure-core-loader []
  (let [glojure-dir (:GLOJURE-DIR make-vars)]
    (str glojure-dir "/pkg/stdlib/clojure/core/loader.go")))

(defn find-glojure-stdlib-dir []
  (let [glojure-dir (:GLOJURE-DIR make-vars)]
    (str glojure-dir "/pkg/stdlib")))

(defn prune? []
  (let [parsed (parse-extensions (or (:ext *opts*) []))]
    (or (contains? parsed "prune")
        (contains? parsed "deps")
        (System/getenv "GLOAT_X_PRUNE"))))

;; Functions referenced at runtime via glj.Var() in main.go templates
;; that won't be found by scanning for var_clojure_DOT_core_ patterns
(def PRUNE-RUNTIME-KEEPS
  ;; Functions directly referenced by main.go template via glj.Var()
  ["require" "alter-var-root" "constantly" "push-thread-bindings"
   ;; Required by Glojure multimethod machinery (lang package calls these
   ;; via glj.Var, invisible to loader block scanning)
   "global-hierarchy" "parents" "isa?"])

;; Namespace require order for ys runtime
(def YS-NS-ORDER
  ["yamlscript.common" "yamlscript.util"
   "ys.fs" "ys.http" "ys.ipc" "ys.json"
   "ys.std" "ys.dwim" "ys.v0"])

(defn ns-to-import-path
  "Convert a dotted namespace to its Go import path for internal/."
  [ns-name go-module]
  (let [pkg-path (cond
                   (str/starts-with? ns-name "ys.")
                   (str "ys/" (subs ns-name 3))
                   (str/starts-with? ns-name "yamlscript.")
                   (str "yamlscript/" (subs ns-name 11))
                   :else
                   (str/replace ns-name "." "/"))]
    (str go-module "/internal/" pkg-path)))

(defn generate-ys-imports
  "Generate Go import lines for used ys namespaces."
  [used-namespaces go-module]
  (let [ordered (filter #(contains? used-namespaces %) YS-NS-ORDER)]
    (str/join "\n"
              (map #(str "\t_ \"" (ns-to-import-path % go-module) "\"")
                   ordered))))

(defn generate-ys-requires
  "Generate Go require.Invoke lines for used ys namespaces."
  [used-namespaces]
  (let [ordered (filter #(contains? used-namespaces %) YS-NS-ORDER)]
    (str/join "\n"
              (map #(str "\trequire.Invoke(lang.NewSymbol(\"" % "\"))")
                   ordered))))

(defn deep-prune
  "Run the full dependency-graph prune using prune.clj.
   Returns the set of used ys/yamlscript namespaces."
  [output-dir go-module source-required-nses]
  (let [stdlib-dir (find-glojure-stdlib-dir)
        parsed (parse-extensions (or (:ext *opts*) []))
        deps-mode (when (contains? parsed "deps")
                    (let [v (get parsed "deps")]
                      (if (string? v)
                        (keyword v)
                        :list)))
        config {:build-dir output-dir
                :gloat-root GLOAT-ROOT
                :stdlib-dir stdlib-dir
                :runtime-keeps PRUNE-RUNTIME-KEEPS
                :deps-mode deps-mode
                :source-required-nses source-required-nses
                :quiet (:quiet *opts*)
                :verbose (:verbose *opts*)}]
    (timer-start)
    (let [result (prune/prune-all config)]
      (timer-end "PRUNE")
      ;; Emit deps output if requested (done here because deps.clj
      ;; is loaded after prune.clj so deps/ ns isn't visible there)
      (when deps-mode
        (let [edges (:edges (:graph-result result))
              roots (deps/find-user-roots edges)]
          (deps/emit-deps deps-mode edges roots (:user-quiet *opts*))
          (System/exit 0)))
      (:used-namespaces result))))

(defn cat-bb [name]
  (let [src (str GLOAT-ROOT "/ys/src/ys/" name ".clj")
        patch (str GLOAT-ROOT "/ys/patch/ys-" name "-bb.patch")
        tmpfile (str (fs/create-temp-file))
        patch-content (slurp patch)]

    (process/shell
     {:in patch-content
      :out :string
      :err :string}
     "patch" "--no-backup-if-mismatch" "-p0" "-o" tmpfile src)

    ;; Special handling for fs and std
    (when (= name "fs")
      (process/shell "bash" (str GLOAT-ROOT "/ys/patch/fix-fs-bb.sh") tmpfile))
    (when (= name "std")
      (process/shell "bash" (str GLOAT-ROOT "/ys/patch/fix-std-bb.sh") tmpfile))

    (let [content (slurp tmpfile)]
      (fs/delete tmpfile)
      content)))

(defn generate-bb [clj-file]
  (let [parts [(slurp (str GLOAT-ROOT "/ys/src/yamlscript/util.clj")) "\n"
               (slurp (str GLOAT-ROOT "/ys/src/yamlscript/common.clj")) "\n"
               (cat-bb "fs") "\n"
               (cat-bb "ipc") "\n"
               (cat-bb "std") "\n"
               (slurp (str GLOAT-ROOT "/ys/src/ys/dwim.clj")) "\n"
               ;; Filter v0.clj
               (-> (slurp (str GLOAT-ROOT "/ys/src/ys/v0.clj"))
                   (str/replace #"(?m)^\s*\[yamlscript\.common\]\n" "")
                   (str/replace #"(?m)^\s*\[ys\.http\]\n" "")
                   (str/replace #"(?m)^\s*\[ys\.json\]\n" "")) "\n"
               ;; Filter generated CLJ
               (-> (slurp clj-file)
                   (str/replace #"\[ys\.http :as http\]" "")
                   (str/replace #"\[ys\.json :as json\]" "")
                   (str/replace #"(?m)^\s*\[ys\.http\]\n" "")
                   (str/replace #"(?m)^\s*\[ys\.json\]\n" ""))
               "\n(apply -main *command-line-args*)\n"]]
    (apply str parts)))

;;------------------------------------------------------------------------------
;; High-Level Orchestrators
;;------------------------------------------------------------------------------

(defn convert-to-stdout [input format namespace]
  (let [tmpdir (str (fs/create-temp-dir))
        input-type (get-file-type input)
        clj-file (str tmpdir "/temp.clj")
        glj-file (str tmpdir "/temp.glj")]

    (try
      ;; Convert to Clojure if needed
      (case input-type
        "ys" (ys-to-clj input clj-file namespace)
        "clj" (fs/copy input clj-file {:replace-existing true})
        "glj" (fs/copy input glj-file {:replace-existing true})
        (die "Unknown input file type: " input))

      ;; Output based on format
      (case format
        "clj" (print (slurp clj-file))
        "bb" (print (generate-bb clj-file))
        "glj" (do
                (when (fs/exists? clj-file)
                  (clj-to-glj clj-file glj-file))
                (print (slurp glj-file)))
        "go" (let [go-tmpdir (str (fs/create-temp-dir))]
               (when (fs/exists? clj-file)
                 (clj-to-glj clj-file glj-file))
               (let [ns (resolve-namespace
                         (or (when
                              (fs/exists? clj-file)
                               clj-file)
                             glj-file)
                         nil)]
                 (glj-to-go glj-file ns go-tmpdir)
                 (let [ns-path (-> ns
                                   (str/replace #"\." "/")
                                   (str/replace #"-" "_"))
                       loader-file (str go-tmpdir "/" ns-path "/loader.go")]
                   (if (fs/exists? loader-file)
                     (print (slurp loader-file))
                     (die "glj compile did not produce loader.go"))
                   (fs/delete-tree go-tmpdir))))
        (die "Format '" format "' requires -o output"))

      (finally
        (fs/delete-tree tmpdir)))))

(defn convert-file [input output format namespace module platform]
  (let [input-type (get-file-type input)]

    ;; For formats that need directory build, delegate
    (if (contains? #{"dir" "bin" "lib" "wasm" "js"} format)
      (let [original-source (str (fs/canonicalize input))
            tmpdir (str (fs/create-temp-dir))
            basename (fs/file-name input)
            ;; Ensure file has appropriate extension
            basename (if-not (re-find #"\.(ys|clj|glj)$" basename)
                       (case input-type
                         "ys" (str basename ".ys")
                         "clj" (str basename ".clj")
                         "glj" (str basename ".glj")
                         basename)
                       basename)]
        (fs/copy input (str tmpdir "/" basename) {:replace-existing true})
        (binding [*source-file* original-source]
          (convert-directory tmpdir output format namespace module platform))
        (fs/delete-tree tmpdir))

      ;; Handle simple file conversion
      (let [tmpdir (str (fs/create-temp-dir))
            clj-file (str tmpdir "/temp.clj")
            glj-file (str tmpdir "/temp.glj")
            ns (when (= input-type "ys")
                 (or namespace (derive-namespace input)))]

        (try
          ;; Stage 1: Convert to Clojure if needed
          (case input-type
            "ys" (do
                   (msg "Converting" input "(.ys) to Clojure...")
                   (ys-to-clj input clj-file ns))
            "clj" (fs/copy input clj-file {:replace-existing true})
            "glj" (fs/copy input glj-file {:replace-existing true})
            (die "Unknown input file type: " input))

          ;; Stage 2: Convert based on format
          (case format
            "clj" (do
                    (fs/copy clj-file output {:replace-existing true})
                    (msg "Generated:" output))
            "bb" (do
                   (spit output (generate-bb clj-file))
                   (msg "Generated:" output))
            "glj" (do
                    (when (fs/exists? clj-file)
                      (msg "Converting Clojure to Glojure...")
                      (clj-to-glj clj-file glj-file))
                    (fs/copy glj-file output {:replace-existing true})
                    (msg "Generated:" output))
            "go" (let [ns (or ns
                              (resolve-namespace
                               (or
                                (when (fs/exists? clj-file) clj-file)
                                glj-file)
                               namespace))]
                   (when (fs/exists? clj-file)
                     (msg "Converting Clojure to Glojure...")
                     (clj-to-glj clj-file glj-file))
                   (msg "Compiling Glojure to Go...")
                   (glj-to-go glj-file ns tmpdir)
                   (let [ns-path (-> ns
                                     (str/replace #"\." "/")
                                     (str/replace #"-" "_"))
                         loader-file (str tmpdir "/" ns-path "/loader.go")]
                     (if (fs/exists? loader-file)
                       (do
                         (fs/copy loader-file output {:replace-existing true})
                         (msg "Generated:" output))
                       (die "glj compile did not produce loader.go at "
                            loader-file)))))

          (finally
            (fs/delete-tree tmpdir)))))))

(defn convert-files [input-files output format namespace module platform]
  "Compile multiple explicit input files to a binary/lib/dir output.
  Each file is copied to a temp directory with a unique name based on its
  namespace to avoid basename collisions (e.g. parser.clj at different depths).
  For lib format, the file with EXPORT is named 'main.clj' so that
  convert-directory selects it as the main namespace."
  (let [tmpdir (str (fs/create-temp-dir))]
    (try
      ;; For lib format, find the file with EXPORT to use as main namespace
      (let [export-file (when (= format "lib")
                          (first (filter
                                  #(not-empty
                                    (or (extract-export (slurp (str %))) []))
                                  input-files)))]
        (doseq [[idx source-file] (map-indexed vector input-files)]
          (let [source-file (str source-file)
                basename (fs/file-name source-file)
                input-type (get-file-type source-file)
                file-ns (when (contains? #{"clj" "glj"} input-type)
                          (parse-namespace source-file))
                ;; Name the EXPORT file 'main' so convert-directory picks it
                ;; as main-namespace; use namespace-derived names for others
                ;; to avoid basename collisions between files at different depths
                unique-name (cond
                              (= source-file (str export-file))
                              (str "main." input-type)

                              file-ns
                              (str (str/replace file-ns #"[.\-]" "_")
                                   "." input-type)

                              :else
                              (str "f" idx "_" basename))]
            (fs/copy source-file (str tmpdir "/" unique-name)
                     {:replace-existing true})))
        (convert-directory tmpdir output format namespace module platform))
      (finally
        (fs/delete-tree tmpdir)))))

(defn convert-directory [input-dir output format namespace module platform]
  (let [is-dir-output (= format "dir")
        is-binary (contains? #{"bin" "lib" "wasm" "js"} format)
        output-dir (cond
                     is-dir-output (str/replace output #"/$" "")
                     is-binary (str (fs/create-temp-dir) "/build")
                     :else (str/replace output #"/$" ""))
        binary-name (when is-binary (fs/file-name output))
        build-mode (when (= format "lib") "-buildmode=c-shared")
        binary-name
        (if (and (= format "lib")
                 (not (str/ends-with? output ".dylib"))
                 (not (str/ends-with? output ".so")))
          (str binary-name ".so")
          binary-name)
        [goos goarch] (cond
                        (= format "wasm") ["wasip1" "wasm"]
                        (= format "js") ["js" "wasm"]
                        platform (str/split platform #"/")
                        :else [nil nil])]

    (msg "Converting directory" input-dir "to" output-dir)
    (fs/create-dirs output-dir)

    ;; Find source files
    (let [source-files (concat
                        (fs/glob input-dir "*.ys" {:max-depth 1})
                        (fs/glob input-dir "*.clj" {:max-depth 1})
                        (fs/glob input-dir "*.glj" {:max-depth 1}))]

      (when (empty? source-files)
        (die "No .ys, .clj, or .glj files found in " input-dir))

      (msg "Found" (count source-files) "source file(s)")

      (let [shared-tmpdir (str (fs/create-temp-dir))
            all-namespaces (atom [])
            main-namespace (atom nil)
            export-map (atom nil)
            has-main (atom false)
            required-nses (atom #{})]

        (try
          ;; Convert each file
          (doseq [source-file source-files]
            (let [basename (fs/file-name source-file)
                  name (str/replace basename #"\.[^.]+$" "")
                  input-type (get-file-type (str source-file))
                  clj-file (str shared-tmpdir "/" name ".clj")
                  glj-file (str shared-tmpdir "/" name ".glj")
                  ns (when (= input-type "ys")
                       (or namespace (derive-namespace (str source-file))))]

              (msg "  Converting" basename "...")

              ;; Convert through pipeline
              (case input-type
                "ys" (ys-to-clj (str source-file) clj-file ns)
                "clj" (fs/copy source-file clj-file {:replace-existing true})
                "glj" (fs/copy source-file glj-file {:replace-existing true})
                (die "Unknown file type: " basename))

              ;; Extract EXPORT, check for main function, and collect
              ;; required namespaces for prune
              (when (fs/exists? clj-file)
                (let [clj-content (slurp clj-file)]
                  (when-let [exports (extract-export clj-content)]
                    (reset! export-map exports))
                  (when (has-main-fn? clj-content)
                    (reset! has-main true))
                  ;; Collect ys/yamlscript namespaces from bare require forms
                  ;; Matches: 'ys.fs and '[ys.http :as http] but NOT
                  ;; (:require [ys.v0 ...]) which is handled by loader scanning
                  (let [nses (re-seq #"'(?:\[)?(ys\.\w+|yamlscript\.\w+)"
                                     clj-content)]
                    (doseq [[_ ns-name] nses]
                      (swap! required-nses conj ns-name)))))

              ;; Clojure to Glojure
              (when (fs/exists? clj-file)
                (clj-to-glj clj-file glj-file))

              ;; Resolve namespace
              (let [ns (or ns (resolve-namespace
                               (or
                                (when (fs/exists? clj-file) clj-file)
                                glj-file)
                               namespace))
                    ns-path (-> ns
                                (str/replace #"\." "/")
                                (str/replace #"-" "_"))
                    ns-dir (if (str/includes? ns-path "/")
                             (subs ns-path 0 (str/last-index-of ns-path "/"))
                             "")
                    ns-file-name (str (last (str/split ns #"\.")) ".glj")]

                (swap! all-namespaces conj ns)

                ;; Copy to namespace structure
                (fs/create-dirs (str shared-tmpdir "/" ns-dir))
                (fs/copy glj-file (str shared-tmpdir "/" ns-path ".glj")
                         {:replace-existing true})

                ;; First file or file named 'main' becomes main namespace
                (when (or (nil? @main-namespace) (= name "main"))
                  (reset! main-namespace ns)))))

          ;; Copy pre-compiled ys runtime and dependencies (GLJ files)
          (let [ys-glj-dir (str GLOAT-ROOT "/ys/glj")]
            (doseq [file (fs/glob ys-glj-dir "**/*")]
              (when (fs/regular-file? file)
                (let [rel-path (str (fs/relativize ys-glj-dir file))
                      target (str shared-tmpdir "/" rel-path)]
                  (fs/create-dirs (fs/parent target))
                  (fs/copy file target {:replace-existing true})))))

          (let [glj (:GLJ make-vars)]
            ;; Compile all user namespaces
            (doseq [ns @all-namespaces]
              (msg "  Compiling" ns "...")
              (let [compile-cmd (str "(compile (quote " ns "))")
                    opts {:in compile-cmd
                          :dir shared-tmpdir
                          :extra-env go-env
                          :out :string
                          :err :string}]
                (try
                  (let [result (process/shell (assoc opts :continue true) glj)]
                    (when-not (zero? (:exit result))
                      (die "glj compile failed for " ns ":\n"
                           (or (not-empty (:err result))
                               (:out result)))))
                  (catch Exception e
                    (die "glj compile failed for " ns ":\n"
                         (.getMessage e)))))))

          ;; Copy generated Go files to output directory under pkg/
          ;; Exclude YS stdlib files (they come from ys/pkg module)
          (fs/create-dirs (str output-dir "/pkg"))
          (doseq [gofile (fs/glob shared-tmpdir "**/*.go")]
            (let [rel-path (str (fs/relativize shared-tmpdir gofile))
                  ;; Skip YS stdlib files - they're provided by ys/pkg module
                  stdlib-paths ["yamlscript/" "ys/"]
                  is-stdlib? (some #(str/starts-with? rel-path %) stdlib-paths)]
              (when-not is-stdlib?
                (let [target (str output-dir "/pkg/" rel-path)]
                  (fs/create-dirs (fs/parent target))
                  (fs/copy gofile target {:replace-existing true})))))

          (when-not @main-namespace
            (die "Could not determine main namespace"))

          (msg "Main namespace:" @main-namespace)

          ;; Validate lib format requirements
          (when (= format "lib")
            (when-not @export-map
              (die "Library format requires EXPORT declaration.\n"
                   "Add (def EXPORT {...}) with exported function signatures."))
            (when @has-main
              (die "Library format cannot have a main function.\n"
                   "Libraries use EXPORT declaration, binaries use main.")))

          ;; Determine Go module name
          (let [go-module (or module
                              (System/getenv "GLOAT_MODULE")
                              (str "github.com/gloathub/"
                                   (fs/file-name output-dir)))]
            (msg "Go module:" go-module)

            ;; Generate go.mod
            (let [glojure-version (:GLOJURE-VERSION make-vars)
                  ys-pkg-version (:YS-PKG-VERSION make-vars)
                  template-content (slurp (str TEMPLATE "/go.mod"))
                  result (render-template
                          template-content
                          [["GO-MODULE" go-module]
                           ["GLOJURE-VERSION" glojure-version]
                           ["YS-PKG-VERSION" ys-pkg-version]
                           ["GLOAT-ROOT" GLOAT-ROOT]])]
              (spit (str output-dir "/go.mod") result)
              (msg "Generated:" (str output-dir "/go.mod")))

            ;; Run deep prune before generating main.go
            ;; (prune needs user's pkg/ files; main.go needs prune results)
            (let [used-ys-ns (when (prune?)
                               (deep-prune output-dir go-module
                                           @required-nses))]

              ;; Generate main.go
              (let [package-path (-> @main-namespace
                                     (str/replace #"\." "/")
                                     (str/replace #"-" "_"))
                    template (cond
                               (and (= format "lib") (prune?))
                               (str TEMPLATE "/lib-main-prune.go")
                               (= format "lib")
                               (str TEMPLATE "/lib-main.go")
                               (prune?)
                               (str TEMPLATE "/main-prune.go")
                               :else
                               (str TEMPLATE "/main.go"))
                    template-content (slurp template)
                    ;; Generate export functions for lib format
                    export-functions (if (= format "lib")
                                       (generate-export-functions
                                        @export-map @main-namespace)
                                       "")
                    ;; Generate dynamic imports/requires for prune mode
                    ys-imports (if used-ys-ns
                                 (generate-ys-imports used-ys-ns go-module)
                                 "")
                    ys-requires (if used-ys-ns
                                  (generate-ys-requires used-ys-ns)
                                  "")
                    ;; Generate blank imports for all compiled namespaces
                    ;; (excluding main) so their init() fns register loaders
                    all-ns-imports
                    (let [main-ns-path package-path
                          other-nses (remove #(= % @main-namespace)
                                             @all-namespaces)
                          stdlib-prefixes ["yamlscript." "ys."]]
                      (str/join "\n"
                                (map (fn [ns]
                                       (let [np (-> ns
                                                    (str/replace #"\." "/")
                                                    (str/replace #"-" "_"))]
                                         (str "\t_ \"" go-module "/pkg/" np "\"")))
                                     (remove
                                      (fn [ns]
                                        (some #(str/starts-with? ns %)
                                              stdlib-prefixes))
                                      other-nses))))
                    ;; Generate require.Invoke calls for all compiled namespaces
                    ;; (excluding main and stdlib) so their vars are bound
                    ;; before any user code runs. Glojure AOT does not generate
                    ;; NSRequire calls for :require forms, so we must do this
                    ;; explicitly.
                    all-ns-requires
                    (let [other-nses (remove #(= % @main-namespace)
                                             @all-namespaces)
                          stdlib-prefixes ["yamlscript." "ys."]]
                      (str/join "\n"
                                (map (fn [ns]
                                       (str "\trequire.Invoke(lang.NewSymbol(\""
                                            ns "\"))"))
                                     (remove
                                      (fn [ns]
                                        (some #(str/starts-with? ns %)
                                              stdlib-prefixes))
                                      other-nses))))
                    result (render-template
                            template-content
                            [["GO-MODULE" go-module]
                             ["PACKAGE-PATH" package-path]
                             ["NAMESPACE" @main-namespace]
                             ["EXPORT-FUNCTIONS" export-functions]
                             ["YS-IMPORTS" ys-imports]
                             ["YS-REQUIRES" ys-requires]
                             ["ALL-NS-IMPORTS" all-ns-imports]
                             ["ALL-NS-REQUIRES" all-ns-requires]])]
                (spit (str output-dir "/main.go") result)
                (msg "Generated:" (str output-dir "/main.go")))

              ;; Generate Makefile for directory output
            (when is-dir-output
              (let [bin-name (or binary-name (fs/file-name output-dir))
                    template-content (slurp (str TEMPLATE "/Makefile"))
                    result (render-template template-content
                                            [["BINARY-NAME" bin-name]])]
                (spit (str output-dir "/Makefile") result)
                (msg "Generated:" (str output-dir "/Makefile"))))

            ;; Build binary if needed
            (if is-binary
              (let [go-bin (:GO make-vars)
                    build-env (merge go-env
                                     {"GONOSUMCHECK" "*"}
                                     (when (= format "lib") {"CGO_ENABLED" "1"})
                                     (when goos {"GOOS" goos})
                                     (when goarch {"GOARCH" goarch}))]

                (msg "Building" format "...")

                (let [io-opts (if (:quiet *opts*)
                                {:out :string :err :string}
                                {:out :inherit :err :inherit})]

                  ;; go mod tidy
                  (process/shell (merge {:dir output-dir
                                         :extra-env build-env}
                                        io-opts)
                                 go-bin "mod" "tidy")

                  ;; Build
                  (timer-start)
                  (let [build-args
                        (concat [go-bin "build"
                                 "-ldflags" "-s -w"
                                 "-o" binary-name]
                                (when (prune?)
                                  ["-tags" "glj_no_aot_stdlib"])
                                (when build-mode [build-mode])
                                ["main.go"])]
                    (apply process/shell (merge {:dir output-dir
                                                 :extra-env build-env}
                                                io-opts)
                           build-args)))
                (timer-end "GO→BIN")

                (let [built-file (str output-dir "/" binary-name)]
                  (if (fs/exists? built-file)
                    (do
                      (fs/copy built-file output {:replace-existing true})
                      (msg "Generated:" output)

                      ;; Compress WASM if needed
                      (let [compress-exts (keys (dissoc (parse-extensions (or (:ext *opts*) []))
                                                        "prune" "html" "serve" "open"))]
                        (when (and (contains? #{"wasm" "js"} format)
                                   (seq compress-exts))
                          (compress-wasm output compress-exts)))

                      (when (= format "js")
                        (let [parsed   (parse-extensions (or (:ext *opts*) []))
                              has-open  (contains? parsed "open")
                              has-serve (or has-open (contains? parsed "serve"))
                              has-html  (or has-serve (contains? parsed "html"))
                              args-val  (some #(let [v (get parsed %)]
                                                 (when (string? v) v))
                                              ["open" "serve" "html"])
                              program-args (if (seq args-val)
                                             (str/split args-val #"\s+") [])
                              config {:output       output
                                      :go-bin       (:GO make-vars)
                                      :template-dir TEMPLATE
                                      :program-args program-args
                                      :quiet        (:quiet *opts*)
                                      :serve        has-serve
                                      :has-html     has-html
                                      :open         has-open
                                      :gloat-root   GLOAT-ROOT}]
                          (when has-html (html/generate config))
                          (when has-serve (serve/serve config))))

                      ;; Copy .h file for shared libraries
                      (when (= format "lib")
                        (let [h-file (-> binary-name
                                         (str/replace #"\.so$" "")
                                         (str/replace #"\.dylib$" "")
                                         (str ".h"))
                              h-output (-> output
                                           (str/replace #"\.so$" "")
                                           (str/replace #"\.dylib$" "")
                                           (str ".h"))
                              h-source (str output-dir "/" h-file)]
                          (when (fs/exists? h-source)
                            (fs/copy h-source h-output {:replace-existing true})
                            (msg "Generated:" h-output))))

                      ;; Clean up temp build dir
                      (fs/delete-tree (fs/parent output-dir)))
                    (die "Build failed"))))
              (do
                ;; Directory output message
                (if is-dir-output
                  (do
                    (msg "Generated Go module in:" output-dir)
                    (if (prune?)
                      (msg "To build: cd" output-dir
                           "&& go build -tags glj_no_aot_stdlib")
                      (msg "To build: cd" output-dir "&& make")))
                  (do
                    (msg "Generated Go module in:" output-dir)
                    (msg "To build: cd" output-dir "&& go build")))))))

          (finally
            (fs/delete-tree shared-tmpdir)))))))

;;------------------------------------------------------------------------------
;; Main Logic
;;------------------------------------------------------------------------------

(defn expand-dir-args
  "Expand any directory arguments to all .clj, .ys, and .glj files within.
  Non-directory arguments are passed through unchanged. Order is preserved:
  directory contents are sorted and inserted at the directory's position."
  [args]
  (mapcat (fn [arg]
            (if (fs/directory? arg)
              (->> (concat (fs/glob arg "**/*.clj")
                           (fs/glob arg "**/*.ys")
                           (fs/glob arg "**/*.glj"))
                   sort
                   (map str))
              [arg]))
          args))

(defn set-vars [opts]
  (let [input (first (:args opts))
        output (:out opts)
        namespace (:ns opts)
        module (or (:module opts) (System/getenv "GLOAT_MODULE"))
        platform (:platform opts)
        to (:to opts)
        run (:run opts)]

    ;; --run implies quiet
    (let [opts (if run
                 (assoc opts :quiet true)
                 opts)]

      ;; --run only supports single input
      (when (and run (> (count (:args opts)) 1))
        (die "--run does not support multiple input files."
             "Use '--' for --run program arguments."))

      ;; Multiple input files: compile them together (requires -o output)
      (when (> (count (:args opts)) 1)
        (let [files (expand-dir-args (:args opts))
              output (:out opts)
              to (:to opts)
              format (infer-format output to)]
          (when-not output
            (die "Multiple input files require -o output"))
          (when (contains? #{"clj" "glj" "go" "bb"} format)
            (die "Multiple input files not supported for format: " format))
          (doseq [f files]
            (when-not (fs/exists? f)
              (die "Input file does not exist: " f)))
          (when (:force opts)
            (when (fs/exists? output)
              (fs/delete-tree output)))
          (when (and output (not (:force opts)))
            (when (fs/exists? output)
              (die "Output already exists: " output
                   " (use --force to overwrite)")))
          (binding [*opts* opts]
            (convert-files files output format namespace module platform))
          (System/exit 0)))

      ;; Validate input
      (let [input (if (and (nil? input) (nil? output))
                    (die "Missing input file")
                    (or input
                        (when output "-")
                        nil))]

        ;; Auto-detect file extension if file doesn't exist
        (let [input (if (and input
                             (not= input "-")
                             (not (fs/exists? input))
                             (not (str/includes? input ".")))
                      (or (first (filter fs/exists? [(str input ".ys")
                                                     (str input ".clj")
                                                     (str input ".glj")]))
                          input)
                      input)]

          (when-not (or (= input "-") (fs/exists? input))
            (die "Input file/directory does not exist: " input))

          ;; Validate platform format
          (when (and platform
                     (not (re-matches #"^[a-z][a-z0-9]*/[a-z0-9]+$" platform)))
            (die (str
                  "Platform must be in format OS/ARCH"
                  "(e.g., linux/amd64, wasip1/wasm)")))

          ;; Handle -t .ext shorthand
          (let [[to output] (if (and to (str/starts-with? to "."))
                              (let [ext (subs to 1)
                                    basename (-> (fs/file-name input)
                                                 (str/replace #"\.[^.]+$" ""))
                                    new-output (or
                                                output
                                                (str basename "." ext))]
                                [ext new-output])
                              [to output])]

            ;; --run without -o: compile to temp file
            (let [[output to]
                  (if (and run (nil? output))
                    (let [run-tmpdir (str (fs/create-temp-dir))]
                      (alter-var-root #'*opts* assoc :run-tmpdir run-tmpdir)
                      (cond
                        (= to "bb") [(str run-tmpdir "/gloat-run.bb") to]
                        (nil? to) [(str run-tmpdir "/gloat-run") "bin"]
                        :else [(str run-tmpdir "/gloat-run." to) to]))
                    [output to])]

              ;; Default: no -o and no -t means binary output
              (let [[output to]
                    (if (and (nil? output) (nil? to))
                      (let [basename (if (= input "-")
                                       "app"
                                       (-> (fs/file-name input)
                                           (str/replace #"\.[^.]+$" "")))]
                        [basename "bin"])
                      [output to])]

                (assoc opts
                       :input input
                       :output output
                       :namespace namespace
                       :module module
                       :platform platform
                       :to to)))))))))

(defn check-exists [output force]
  (when (and output force)
    (when (fs/exists? output)
      (if (fs/directory? output)
        (msg "Removing" (str output "/") "...")
        (msg "Removing" output "..."))
      (fs/delete-tree output))
    (when (and (str/ends-with? output "/")
               (fs/exists? (str/replace output #"/$" "")))
      (msg "Removing" (str/replace output #"/$" "") "/...")
      (fs/delete-tree (str/replace output #"/$" "")))))

(defn print-verbose-header [opts format]
  (when (and (:verbose opts) (:output opts) (not (:quiet opts)))
    (let [input-name (fs/file-name (:input opts))]
      (binding [*out* *err*]
        (println "Compiling" input-name "to" (str format "..."))))
    (alter-var-root #'*compile-start* (constantly (System/currentTimeMillis)))))

(defn print-verbose-footer [opts]
  (when (and (:verbose opts) (:output opts) (not (:quiet opts)) *compile-start*)
    (let [total-time (- (System/currentTimeMillis) *compile-start*)]
      (binding [*out* *err*]
        (println "done" (str "(" total-time "ms)"))))))

(defn -main [& args]
  (when-not VERSION
    (die "gloat.clj not called from gloat"))

  (setup)

  (let [parsed-opts (parse-opts (vec args))]
    (binding [*opts* parsed-opts]
      (do-version)
      (do-formats)
      (do-extensions)
      (do-platforms)
      (do-shell)
      (do-shell-all)
      (do-reset)
      (do-upgrade)
      (do-complete)
      (validate-extensions)

      (let [opts (set-vars parsed-opts)
            format (infer-format (:output opts) (:to opts))
            deps-only (contains? (parse-extensions (or (:ext opts) []))
                                 "deps")
            opts (if deps-only
                   (assoc opts
                          :user-quiet (:quiet opts)
                          :quiet true :verbose false)
                   opts)]

        (binding [*opts* opts]
          (check-exists (:output opts) (:force opts))

          ;; Fail fast if output already exists (unless --force or deps-only)
          (when (and (:output opts) (not (:force opts)) (not deps-only))
            (when (fs/exists? (:output opts))
              (die "Output already exists: " (:output opts)
                   " (use --force to overwrite)"))
            (when (and (str/ends-with? (:output opts) "/")
                       (fs/exists? (str/replace (:output opts) #"/$" "")))
              (die "Output already exists: "
                   (str/replace (:output opts) #"/$" "")
                   " (use --force to overwrite)")))

          (print-verbose-header opts format)

          ;; Dispatch based on input/output
          (cond
            (nil? (:output opts))
            (if (contains? #{"clj" "bb" "glj" "go"} format)
              (convert-to-stdout
               (:input opts) format (or (:namespace opts) "main.core"))
              (die "Format '" format "' requires -o output"))

            (fs/regular-file? (:input opts))
            (convert-file
             (:input opts)
             (:output opts)
             format
             (:namespace opts)
             (:module opts)
             (:platform opts))

            (fs/directory? (:input opts))
            (convert-directory
             (:input opts)
             (:output opts)
             format
             (:namespace opts)
             (:module opts)
             (:platform opts))

            (= (:input opts) "-")
            (let [content (slurp *in*)
                  clj? (re-find #"^\s*\(" content)
                  suffix (if clj? ".clj" ".ys")
                  content (if (and clj? (not (re-find #"(?m)^\s*\(ns\s" content)))
                            (str "(ns main.core)\n" content)
                            content)
                  tmpfile (str (fs/create-temp-file {:suffix suffix}))]
              (spit tmpfile content)
              (convert-file
               tmpfile
               (:output opts)
               format
               (:namespace opts)
               (:module opts)
               (:platform opts))
              (fs/delete tmpfile))

            :else
            (die "Invalid input: " (:input opts)))

          ;; Execute compiled output if --run
          (when (:run opts)
            (when-not (fs/exists? (:output opts))
              (die "Compilation failed - no output to run"))

            (let [rc
                  (case format
                    "bb"
                    (let [bb (:BB make-vars)]
                      (when-not (fs/executable? bb)
                        (die (str
                              "Babashka not found"
                              "(run 'make shell' to install)")))
                      (let [result
                            (apply process/shell {:continue true}
                                   bb (:output opts) (:run-args opts))]
                        (:exit result)))

                    ("bin" "lib" "wasm" "js")
                    (let [result (apply process/shell {:continue true}
                                        (:output opts) (:run-args opts))]
                      (:exit result))

                    (die "Format '" format
                         "' cannot be executed with --run"))]

              (when (:run-tmpdir opts)
                (fs/delete-tree (:run-tmpdir opts)))
              (System/exit rc)))

          (print-verbose-footer opts))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
