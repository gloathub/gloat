#!/usr/bin/env bb

;; gloat - Glojure Automation Tool
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
  (str (fs/parent (fs/parent (fs/canonicalize *file*)))))

(load-file (str GLOAT-ROOT "/src/prune.clj"))
(load-file (str GLOAT-ROOT "/src/deps.clj"))
(load-file (str GLOAT-ROOT "/src/html.clj"))
(load-file (str GLOAT-ROOT "/src/open.clj"))
(load-file (str GLOAT-ROOT "/src/serve.clj"))
(load-file (str GLOAT-ROOT "/src/report.clj"))

(def TEMPLATE (str GLOAT-ROOT "/template"))
(def GLOAT-TMP (str GLOAT-ROOT "/.cache/local/tmp"))
(fs/create-dirs GLOAT-TMP)

(def VALID-EXTENSIONS #{"gzip" "brotli" "prune" "deps" "html" "serve" "open" "goimports" "report"})

(def go-env
  {"GOPATH"     (str GLOAT-ROOT "/.cache/local/go")
   "GOMODCACHE" (str GLOAT-ROOT "/.cache/local/go/pkg/mod")
   "GOCACHE"    (str GLOAT-ROOT "/.cache/local/cache/go-build")})

(defn prepend-glj-classpath [env path]
  (let [existing (or (get env "GLJ_CLASSPATH")
                     (System/getenv "GLJ_CLASSPATH"))
        sep (System/getProperty "path.separator")]
    (assoc env "GLJ_CLASSPATH"
           (if (str/blank? existing)
             path
             (str path sep existing)))))

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

(defn ys-v0-source-dir []
  (str (:YS-V0-GO-DIR make-vars) "/source"))

(defn setup []
  (let [result (process/shell
                 {:out :string
                  :dir GLOAT-ROOT
                  :extra-env go-env}
                 "make" "--quiet" "--no-print-directory"
                 "gloat-vars")
        out (str/trim (:out result))
        ;; Dependency install recipes may print progress lines before
        ;; the vars; keep only the trailing EDN map.
        vars-edn (re-find #"(?s)\{[^{}]*\}\z" out)]
    (when-not vars-edn
      (die (str "'make gloat-vars' did not produce an EDN map:\n" out)))
    (alter-var-root #'make-vars
      (constantly (edn/read-string vars-edn)))))

;;------------------------------------------------------------------------------
;; gljdeps.edn Resolution
;;------------------------------------------------------------------------------

(defn resolve-deps-file
  "Locate gljdeps.edn. Precedence: --deps= flag, GLOAT_GLJDEPS env var,
  ./gljdeps.edn in CWD. Returns absolute path string, or nil if none.
  Dies if an explicitly-named file is missing."
  []
  (let [explicit (or (:deps *opts*)
                     (System/getenv "GLOAT_GLJDEPS"))
        cwd-file (str (fs/cwd) "/gljdeps.edn")]
    (cond
      explicit
      (do
        (when-not (fs/exists? explicit)
          (die "Error: " explicit " not found"))
        (str (fs/absolutize explicit)))

      (fs/exists? cwd-file)
      cwd-file

      :else nil)))

(defn- deps-stripped
  "Strip EDN comments and whitespace; used to detect empty deps files."
  [content]
  (-> content
      (str/replace #";[^\n]*" "")
      (str/replace #"\s+" "")))

(defn- normalize-dep-key
  "Glojure dep keys use ':' to escape '/' in Go import paths. Rewrite
  back to slashes. Accepts symbol or string."
  [k]
  (-> k str (str/replace #":" "/")))

(defn parse-gljdeps
  "Read a gljdeps.edn file and return a vector of [module-path version]
  pairs for the deps it declares. Validates emptiness and structure the
  same way prepare-repl-env does in bin/gloat."
  [path]
  (let [content (slurp path)]
    (when (str/blank? (deps-stripped content))
      (die (str "Error: " path " has no edn content.\n"
                "\n"
                "A minimal gljdeps.edn declares zero or more Go module deps as edn. Try:\n"
                "\n"
                "  {:deps {}}                  ; no extra deps\n"
                "  {:deps {github.com:google:uuid {:mvn/version \"v1.6.0\"}}}")))
    (let [edn (try
                (edn/read-string {:default (fn [_tag v] v)} content)
                (catch Exception e
                  (die "Error parsing " path ": " (.getMessage e))))
          deps (:deps edn)]
      (when-not (map? deps)
        (die "Error: " path " must contain a {:deps {...}} map"))
      (mapv (fn [[k v]]
              (let [path-str (normalize-dep-key k)
                    version  (:mvn/version v)]
                (when-not version
                  (die "Error: " path ": dep " k
                       " missing :mvn/version"))
                [path-str version]))
            deps))))

(defn render-extra-deps
  "Render parsed deps as a require(...) block for go.mod. Returns empty
  string when there are no deps (template absorbs the empty line)."
  [deps]
  (if (seq deps)
    (str "require (\n"
         (str/join "\n"
                   (map (fn [[path version]]
                          (str "\t" path " " version))
                        deps))
         "\n)\n")
    ""))

(defn- parse-glojure-replaces
  "Extract replace directives from glojure's go.mod so the same fork
  pinning bash's setup-go-module installs is honoured in AOT too."
  [glojure-mod-path]
  (let [text (slurp glojure-mod-path)
        block (some->> (re-find #"(?s)replace\s*\(([^)]*)\)" text) second)
        block-lines (when block
                      (->> (str/split-lines block)
                           (map #(str/replace % #"\s*//.*" ""))
                           (map str/trim)
                           (remove str/blank?)))
        single-lines (->> (str/split-lines text)
                          (map #(str/replace % #"\s*//.*" ""))
                          (map str/trim)
                          (filter #(re-find #"^replace\s+\S" %))
                          (map #(str/replace % #"^replace\s+" "")))]
    (->> (concat block-lines single-lines)
         (keep (fn [line]
                 (when-let [[_ left right]
                            (re-find #"^(\S.*?)\s*=>\s*(\S.*?)$" line)]
                   [(str/trim left) (str/trim right)]))))))

(defn go-mod-version
  "Ensure a module version string has the `v` prefix Go requires."
  [v]
  (if (and v (str/starts-with? v "v")) v (str "v" v)))

(defn write-glj-workspace-mod
  "Write a go.mod into the glj compile workspace so `glj compile` can
  `go get` user-declared deps and build the generated wrapper code.
  Mirrors what prepare-repl-env's setup-go-module produces."
  [tmpdir glojure-dir glojure-version extra-deps]
  (let [replaces (parse-glojure-replaces (str glojure-dir "/go.mod"))
        replace-lines (->> replaces
                           (map (fn [[old new]]
                                  (str "replace " old " => " new)))
                           (str/join "\n"))
        extra-block (render-extra-deps extra-deps)
        body (str "module gloataot\n\n"
                  "go 1.24\n\n"
                  "require github.com/glojurelang/glojure " (go-mod-version glojure-version)
                  "\n\n"
                  "replace github.com/glojurelang/glojure => " glojure-dir "\n"
                  (when (seq replaces) (str replace-lines "\n"))
                  (when (seq extra-block) (str "\n" extra-block)))]
    (spit (str tmpdir "/go.mod") body)
    (let [glj-sum (str glojure-dir "/go.sum")]
      (when (fs/exists? glj-sum)
        (fs/copy glj-sum (str tmpdir "/go.sum")
                 {:replace-existing true})))))

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

(def known-engines
  #{"glojure" "graalvm" "jolt"
    "let-go-vm" "let-go-lower-vm" "let-go-lower"})

(def engine-aliases {"glj"   "glojure"
                     "lgvm"  "let-go-vm"
                     "lglvm" "let-go-lower-vm"
                     "lgl"   "let-go-lower"})

;; let-go-vm runs/bundles bytecode on the let-go VM; the "lg" format
;; is its source form (.lg).
(def lg-engine-formats #{"lg" "bin" "lib"})

;; let-go-lower-vm compiles to native Go via let-go's AOT lowering
;; with the VM along for non-lowered code (trampolines); the "LG"
;; format emits the lowered Go source. let-go-lower (planned) will
;; prune the VM out entirely: pure lowered Go, no trampoline.
(def LG-engine-formats #{"LG" "bin" "lib"})

;; GraalVM Native Image currently builds host-native Clojure executables.
(def graal-engine-formats #{"bin"})

;; Jolt currently builds host-native Clojure executables.
(def jolt-engine-formats #{"bin"})

(defn resolve-engine [opts]
  (let [engine (or (:engine opts)
                   (not-empty (System/getenv "GLOAT_ENGINE"))
                   "glojure")
        engine (get engine-aliases engine engine)]
    (when-not (contains? known-engines engine)
      (die "Unknown engine '" engine "'. Known engines: "
           (str/join ", "
                     (map (fn [e]
                            (if-let [alias (some (fn [[a c]]
                                                   (when (= c e) a))
                                                 engine-aliases)]
                              (str e " (" alias ")")
                              e))
                          (sort known-engines)))))
    (when (= "let-go-lower" engine)
      (die "Engine 'let-go-lower' is not yet implemented"
           " (try let-go-lower-vm)"))
    engine))

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
    (str/ends-with? file ".dll") "lib"
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
    (str/ends-with? output ".lg") "lg"
    (str/ends-with? output ".clj") "clj"
    (str/ends-with? output ".glj") "glj"
    (str/ends-with? output ".go") "go"
    (str/ends-with? output ".so") "lib"
    (str/ends-with? output ".dylib") "lib"
    (str/ends-with? output ".dll") "lib"
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

(defn extract-replaces
  "Return the replace directives from a go.mod as a seq of [old new] strings.
  Handles both single-line `replace X => Y` and block `replace ( ... )` forms."
  [go-mod-content]
  (let [strip-comment #(str/trim (str/replace % #"//.*$" ""))
        parse-line (fn [line]
                     (let [parts (str/split line #"=>")]
                       (when (= 2 (count parts))
                         [(str/trim (first parts))
                          (str/trim (second parts))])))
        lines (str/split-lines go-mod-content)]
    (loop [in-block false, out [], xs lines]
      (if (empty? xs)
        out
        (let [raw (first xs), line (strip-comment raw)]
          (cond
            (and in-block (re-find #"^\)" line))
            (recur false out (rest xs))

            (re-find #"^replace\s*\(" line)
            (recur true out (rest xs))

            (and in-block (seq line))
            (recur true (if-let [p (parse-line line)] (conj out p) out)
                   (rest xs))

            (re-find #"^replace\s" line)
            (let [body (str/replace line #"^replace\s+" "")]
              (recur false (if-let [p (parse-line body)] (conj out p) out)
                     (rest xs)))

            :else
            (recur in-block out (rest xs))))))))

(defn has-main-fn? [clj-content]
  "Check if Clojure code contains a (defn main ...) or (defn -main ...)."
  (boolean (re-find #"\(defn\s+-?main\b" clj-content)))

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
        return-type (when (seq type-vec) (last type-vec))
        arg-types (if (> (count type-vec) 1)
                    (butlast type-vec)
                    [])
        return-info (when return-type (get type-mappings return-type))
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
        no-return (or (nil? return-info) (:no-return return-info))
        signature (if no-return
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

        body (if no-return
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

(def lg-type-mappings
  "Map EXPORT type keywords to cgo and let-go VM conversions."
  {:int    {:go-type "C.longlong"
            :go-to-lg "vm.Int(arg)"
            :lg-to-go "C.longlong(result.(vm.Int))"}
   :float  {:go-type "C.double"
            :go-to-lg "vm.Float(arg)"
            :lg-to-go "C.double(result.(vm.Float))"}
   :str    {:go-type "*C.char"
            :go-to-lg "vm.String(C.GoString(arg))"
            :lg-to-go "C.CString(string(result.(vm.String)))"}
   :bool   {:go-type "C.int"
            :go-to-lg "vm.Boolean(arg != 0)"
            :lg-to-go-special true}
   :null   {:no-return true}})

(defn generate-lg-export-function [fn-name type-spec]
  "Generate a //export wrapper that invokes a var in a let-go bundle."
  (let [type-vec (if (vector? type-spec) type-spec [type-spec])
        return-type (when (seq type-vec) (last type-vec))
        arg-types (if (> (count type-vec) 1)
                    (butlast type-vec)
                    [])
        return-info (when return-type (get lg-type-mappings return-type))
        c-fn-name (kebab-to-snake fn-name)
        params (str/join
                ", "
                (map-indexed
                 (fn [idx arg-type]
                   (let [type-info (get lg-type-mappings arg-type)]
                     (str "arg" idx " " (:go-type type-info))))
                 arg-types))
        arg-conversions
        (map-indexed
         (fn [idx arg-type]
           (let [conversion (:go-to-lg (get lg-type-mappings arg-type))]
             (str/replace conversion "arg" (str "arg" idx))))
         arg-types)
        no-return (or (nil? return-info) (:no-return return-info))
        signature (if no-return
                    (str "func " c-fn-name "(" params ")")
                    (str "func " c-fn-name "(" params ") "
                         (:go-type return-info)))
        invoke (str "invoke(\"" fn-name "\""
                    (when (seq arg-conversions)
                      (str ", " (str/join ", " arg-conversions)))
                    ")")
        body (if no-return
               (str "\t" invoke "\n")
               (str "\tresult := " invoke "\n"
                    (if (:lg-to-go-special return-info)
                      (str "\tif result.(vm.Boolean) == vm.TRUE {\n"
                           "\t\treturn 1\n"
                           "\t}\n"
                           "\treturn 0\n")
                      (str "\treturn " (:lg-to-go return-info) "\n"))))]
    (str "//export " c-fn-name "\n"
         signature " {\n"
         body
         "}\n")))

(defn generate-lg-export-functions [export-map]
  "Generate all let-go-backed //export wrappers from EXPORT map."
  (if (empty? export-map)
    ""
    (str/join "\n"
              (map (fn [[fn-name type-spec]]
                     (generate-lg-export-function
                      (name fn-name) type-spec))
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
  lg        let-go source file (self-contained; lg engine)
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
   .js → js     .wasm → wasm    .lg → lg
     / → dir    <none> → bin   .exe → bin")
    (System/exit 0)))

(defn do-engines []
  (when (:engines *opts*)
    (println "Available compilation engines (use with -E/--engine):

  glj     Glojure (default)

  jolt    Jolt (binaries only)

  lgvm    let-go bytecode VM
  lglvm   let-go native lowering with VM fallback
  lgl     let-go native lowering (not yet implemented)

  graalvm GraalVM Native Image (binaries only)")
    (System/exit 0)))

(defn do-extensions []
  (when (:extensions *opts*)
    (println "Available processing extensions (use with -X/--ext):

  report      Write binary size analysis report (-Xreport, -Xreport=html, -Xreport=open)
  brotli      Compress with brotli (auto-installed if needed)
  deps        Print flat dependency list (implies prune)
  deps=tree   Print dependency tree (implies prune)
  goimports   Include Go stdlib in pkgmap (needed for runtime Go interop)
  gzip        Compress with gzip (requires gzip command)
  html        Generate HTML page for js/wasm (-Xhtml or -Xhtml='args')
  open        Open browser after serving (-Xopen or -Xopen='args')
  prune       Prune unused clojure.core functions (smaller binaries)
  serve       Start a local HTTP server after building (-Xserve)

The compression extensions are applied to WASM output formats (wasm, js).
The html, serve, and open extensions are only valid with js format (-o foo.js or -t js).
The prune extension applies to binary builds (bin, lib, wasm, js, dir).
The goimports extension applies to binary builds (bin, lib, wasm, js, dir).

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
  plan9      amd64, 386, arm
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

(defn find-ys
  "Path to the YAMLScript compiler, installing it only when compiling
  YAMLScript source."
  []
  (let [ys (:YS make-vars)]
    (if (and ys (fs/executable? ys))
      ys
      (let [result (process/shell {:out :string :err :string
                                   :continue true
                                   :dir GLOAT-ROOT
                                   :extra-env go-env}
                                  "make" "--quiet" "--no-print-directory"
                                  "path-ys")
            path (->> (str/split-lines (str (:out result)))
                      (remove str/blank?)
                      last)]
        (when-not (and (zero? (:exit result))
                       path (fs/executable? path))
          (die (str "Failed to install ys:\n"
                    (:out result) (:err result))))
        path))))

(defn ys-to-clj [input output namespace]
  (let [ys (find-ys)]
    (timer-start)

    ;; Compile YS to Clojure
    (let [result (process/shell
                  {:out :string
                   :extra-env go-env}
                  ys "-c" input)
          body (-> (->> (:out result)
                        str/split-lines
                        (remove #(= % "(apply main ARGS)"))
                        ;; ys.v0/init installs the standard namespace aliases.
                        ;; Drop compiler-emitted legacy requires that would
                        ;; try to replace those aliases with ys.* bridge names.
                        (remove #(re-matches
                                   #"\(require '\[ys\.(?:fs|http|ipc|json|std|dwim)\b.*\]\)"
                                   %))
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
  (let [args (mapv
               (fn [s]
                 (if (re-matches
                       #\"^[+-]?[0-9]+\\.?[0-9]*([eE][+-]?[0-9]+)?$\" s)
                   (read-string s)
                   s))
               argv)]
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

    ;; Copy the patched portable ys.v0 source tree into the writable compile
    ;; workspace. Glojure analyzes these sources while the final program links
    ;; their precompiled loaders from github.com/gloathub/ys-v0-go.
    (let [ys-source-dir (ys-v0-source-dir)]
      (doseq [file (fs/glob ys-source-dir "**/*")]
        (when (fs/regular-file? file)
          (let [rel-path (str (fs/relativize ys-source-dir file))
                target (str output-dir "/" rel-path)]
            (fs/create-dirs (fs/parent target))
            (fs/copy file target {:replace-existing true})))))

    ;; Compile user namespace only
    (let [compile-cmd (str "(compile (quote " namespace "))")
          opts {:in compile-cmd
                :dir output-dir
                :extra-env (prepend-glj-classpath go-env output-dir)}
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

(defn goimports? []
  (let [parsed (parse-extensions (or (:ext *opts*) []))]
    (contains? parsed "goimports")))

(defn aot-build-tags []
  (concat
   ["glj_aot_runtime"]
   (when (and (not (goimports?)) (not (prune?)))
     ["glj_no_goimports"])
   (when (prune?) ["glj_no_aot_stdlib"])))

(defn report-ext []
  (let [parsed (parse-extensions (or (:ext *opts*) []))
        val (get parsed "report")]
    (when val
      (let [params (if (true? val) []
                     (str/split val #"\+"))
            md-files (filter #(str/ends-with? % ".md") params)
            html-files (filter #(str/ends-with? % ".html") params)
            open? (some #(= % "open") params)
            html? (or open? (some #(= % "html") params) (seq html-files))
            keep? (some #(= % "keep") params)
            unknown (remove #(or (= % "keep")
                                 (= % "html")
                                 (= % "open")
                                 (str/ends-with? % ".md")
                                 (str/ends-with? % ".html")) params)]
        (when (seq unknown)
          (die (str "Unknown -Xreport parameter: " (first unknown))))
        (when (> (count md-files) 1)
          (die "Multiple .md files in -Xreport"))
        (when (> (count html-files) 1)
          (die "Multiple .html files in -Xreport"))
        (let [default-path (if html? "report.html" "report.md")]
          {:path (or (first html-files) (first md-files) default-path)
           :format (if html? :html :md)
           :open (boolean open?)
           :keep (boolean keep?)})))))

;; Functions referenced at runtime via glj.Var() in main.go templates
;; that won't be found by scanning for var_clojure_DOT_core_ patterns
(def PRUNE-RUNTIME-KEEPS
  ;; Functions directly referenced by main.go template via glj.Var()
  ["require" "alter-var-root" "constantly" "push-thread-bindings"
   ;; Required by Glojure multimethod machinery (lang package calls these
   ;; via glj.Var, invisible to loader block scanning)
   "global-hierarchy" "parents" "isa?"])

(defn ys-ns-order []
  (->> (slurp (str (:YS-V0-GO-DIR make-vars) "/runtime/namespaces.edn"))
       edn/read-string
       (mapv str)))

(defn ns-to-import-path
  "Convert a dotted namespace to its Go import path for internal/."
  [ns-name go-module]
  (let [pkg-path (-> ns-name
                     (str/replace "." "/")
                     (str/replace "-" "_"))]
    (str go-module "/internal/" pkg-path)))

(defn generate-ys-imports
  "Generate Go import lines for used ys namespaces."
  [used-namespaces go-module]
  (let [ordered (filter #(contains? used-namespaces %) (ys-ns-order))]
    (str/join "\n"
              (map #(str "\t_ \"" (ns-to-import-path % go-module) "\"")
                   ordered))))

(defn generate-ys-requires
  "Generate Go require.Invoke lines for used ys namespaces."
  [used-namespaces]
  (let [ordered (filter #(contains? used-namespaces %) (ys-ns-order))]
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
                :ys-v0-go-dir (:YS-V0-GO-DIR make-vars)
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

(defn generate-bb [clj-file]
  (str (slurp (str (:YS-V0-GO-DIR make-vars) "/bb/runtime.clj"))
         "\n"
         (slurp clj-file)
         "\n(apply -main *command-line-args*)\n"))

(defn find-lg
  "Path to the lg binary, installing it on demand (lg is a managed
  command like wasmtime, not a core dependency)."
  []
  (let [lg (:LG make-vars)]
    (if (and lg (fs/executable? lg))
      lg
      (let [result (process/shell {:out :string :err :string
                                   :continue true
                                   :dir GLOAT-ROOT
                                   :extra-env go-env}
                                  "make" "--quiet" "--no-print-directory"
                                  "path-lg")
            path (->> (str/split-lines (str (:out result)))
                      (remove str/blank?)
                      last)]
        (when-not (and (zero? (:exit result))
                       path (fs/executable? path))
          (die (str "Failed to install lg:\n"
                    (:out result) (:err result))))
        path))))

(defn find-lg-dev
  "Build or reuse lg from LET-GO-SRC for the lower-vm engine."
  []
  (let [result (process/shell {:out :string :err :string
                               :continue true
                               :dir GLOAT-ROOT
                               :extra-env go-env}
                              "make" "--quiet" "--no-print-directory"
                              "path-lg-dev")
        path (->> (str/split-lines (str (:out result)))
                  (remove str/blank?)
                  last)]
    (when-not (and (zero? (:exit result))
                   path (fs/executable? path))
      (die (str "Failed to build lg from LET-GO-SRC:\n"
                (:out result) (:err result))))
    path))

(def lg-source-paths (str GLOAT-ROOT "/ys/lg:."))

(defn generate-lg-body [clj-file]
  ;; The ys stdlib is NOT inlined: lg resolves (:require [ys.v0 ...])
  ;; from source paths, so run output with ys/lg/ on LG_SOURCE_PATHS
  ;; (lg -b embeds the resolved namespaces in the bundle).
  ;; The str alias comes for free on ys/bb (sci default aliases) but
  ;; lg needs it required explicitly.
  (-> (slurp clj-file)
      (str/replace-first
       "(:require ys.v0)"
       "(:require [ys.v0 :refer :all]\n   [clojure.string :as str])")
      (str/replace #"(?m)^\(ys\.v0/init\)\n+" "")))

(defn generate-lg [clj-file]
  ;; The *compiling-aot* guard keeps `lg -c/-b/-w` from running the
  ;; program while compiling it (compiling executes top-level forms);
  ;; the resolve guard tolerates programs with no -main.
  (str (generate-lg-body clj-file)
       "\n(when-not *compiling-aot*
  (when-let [main (resolve '-main)]
    (apply main *command-line-args*)))\n"))

;;------------------------------------------------------------------------------
;; LG Engine (native Go via let-go AOT lowering)
;;------------------------------------------------------------------------------

(defn find-let-go-src
  "Path to a let-go source checkout: the LG engine needs it for
  scripts/lg-compile, the gogen source path, and the go.mod replace.
  A sibling checkout is preferred for development; installed copies
  clone the pinned source into Gloat's cache on demand."
  []
  (let [src (:LET-GO-SRC make-vars)]
    (if (and src (not (str/blank? src)) (fs/directory? src))
      (str src)
      (let [result (process/shell {:out :string :err :string
                                   :continue true
                                   :dir GLOAT-ROOT}
                                  "make" "--quiet" "--no-print-directory"
                                  "path-let-go-src")
            path (->> (str/split-lines (str (:out result)))
                      (remove str/blank?)
                      last)]
        (when-not (and (zero? (:exit result))
                       path (fs/directory? path))
          (die (str "Failed to install let-go source:\n"
                    (:out result) (:err result))))
        path))))

(defn find-LG-lg
  "Return an lg encoder that matches the source used by lower-vm.
  Managed installs pair the released lg with the exact cached tag;
  development checkouts build lg from that checkout. Both paths verify
  that the encoder commit is the source commit before lowering."
  [let-go-src]
  (let [managed-src (str GLOAT-ROOT "/.cache/let-go")
        managed? (= (str (fs/canonicalize let-go-src))
                    (str (fs/canonicalize managed-src)))
        lg (if managed? (find-lg) (find-lg-dev))
        lg-result (process/shell {:out :string :err :string :continue true}
                                 lg "--version")
        lg-text (str (:out lg-result) (:err lg-result))
        lg-commit (second (re-find #"\(([0-9a-f]{7,40})\)" lg-text))
        src-result (process/shell {:out :string :err :string :continue true}
                                  "git" "-C" let-go-src "rev-parse" "HEAD")
        src-commit (str/trim (str (:out src-result)))]
    (when-not (and (zero? (:exit lg-result)) lg-commit
                   (zero? (:exit src-result))
                   (str/starts-with? src-commit lg-commit))
      (die (str "let-go toolchain mismatch:\n"
                "  encoder: " (str/trim lg-text) "\n"
                "  runtime source: "
                (if (str/blank? src-commit) let-go-src src-commit) "\n"
                "Rebuild both from the same let-go revision.")))
    (when managed?
      (let [dirty (process/shell {:out :string :err :string :continue true}
                                 "git" "-C" let-go-src "status"
                                 "--porcelain" "--untracked-files=no")]
        (when-not (and (zero? (:exit dirty))
                       (str/blank? (str (:out dirty))))
          (die "Managed let-go source has local changes; run gloat --reset."))))
    lg))

(def LG-main-pattern
  #"(?s)\(defn -main \[& argv\]\s*\(let \[args \(map-parse argv\)\](.*?)\(apply main args\)\)\)")

(defn LG-rewrite-main
  "Rewrite the ys-generated -main wrapper into a lowering-friendly
  shape. The original let/alter-var-root body trips let-go lowering
  bugs (invalid Go: redeclared args, mistyped invokes), so the
  dynamic-var setup moves to load-time top-level forms (which run as
  bytecode when the ns chunk loads, after main.go has published
  *command-line-args*) and -main keeps only the apply, which lowers
  cleanly. No-op when the wrapper shape isn't recognized."
  [body]
  (if-let [m (re-find LG-main-pattern body)]
    (let [inner (second m)
          file (second (re-find #"#'FILE \(constantly (\"[^\"]*\")\)"
                                inner))
          dir (second (re-find #"#'DIR \(constantly (\"[^\"]*\")\)"
                               inner))
          repl (str
                "(alter-var-root #'ARGV"
                " (constantly (vec *command-line-args*)))\n"
                "(alter-var-root #'ARGS"
                " (constantly (map-parse (vec *command-line-args*))))\n"
                (when file
                  (str "(alter-var-root #'FILE (constantly "
                       file "))\n"))
                (when dir
                  (str "(alter-var-root #'DIR (constantly "
                       dir "))\n"))
                ;; 0/1-arg calls dispatch DIRECTLY (not via apply) so
                ;; they lower to static Go calls into the lowered
                ;; main, entering the native island; apply through a
                ;; var is dynamic and stays on the VM. The local must
                ;; NOT be named args: the lowered variadic param is
                ;; always Go-named args and the collision emits
                ;; invalid Go (let-go lowering bug).
                "\n(defn -main [& argv]\n"
                "  (let [parsed (map-parse (vec argv))]\n"
                "    (if (empty? parsed)\n"
                "      (main)\n"
                "      (if (empty? (rest parsed))\n"
                "        (main (first parsed))\n"
                "        (apply main parsed)))))")]
      (str/replace-first body LG-main-pattern
                         (str/re-quote-replacement repl)))
    body))

(defn LG-ns-path [prog-ns]
  (-> prog-ns
      (str/replace "." "/")
      (str/replace "-" "_")))

(defn LG-program-ns
  "The (ns ...) name of the generated program; the lowered package
  registers under it and the resolver loads it by path, so it must
  exist and must not be the bare name 'core' (the bundle decoder
  treats an NS literally named core as the main chunk)."
  [body]
  (let [prog-ns (second (re-find #"\(ns\s+([\w.\-]+)" body))]
    (when-not prog-ns
      (die "let-go input must have a namespace"))
    (when (= "core" prog-ns)
      (die "let-go engines can't use the bare namespace 'core'"))
    prog-ns))

(defn lower-lg-to-go
  "Run let-go's lg-compile driver over lg-file, emitting lowered Go
  packages under out-dir with import prefix <go-module>/lowered.
  Returns the emitted .go file paths; empty means nothing lowered
  (the program still runs, pure bytecode)."
  [lg-file src-dir out-dir go-module]
  (let [let-go-src (find-let-go-src)
        lg (find-LG-lg let-go-src)
        ;; let-go's deps.edn supplies pkg/rt/gogen when run from its
        ;; repo root; an explicit LG_SOURCE_PATHS bypasses that, so
        ;; list it here (lg-compile loads gogen.lg on demand).
        source-paths (str src-dir ":" GLOAT-ROOT "/ys/lg:"
                          let-go-src "/pkg/rt/gogen")
        result (process/shell {:out :string :err :string :continue true
                               :extra-env
                               {"LG_SOURCE_PATHS" source-paths}}
                              lg (str let-go-src "/scripts/lg-compile")
                              out-dir (str go-module "/lowered") lg-file)
        out-text (str (:out result) (:err result))]
    (when-not (zero? (:exit result))
      (die (str "lg-compile failed:\n" out-text
                (when (re-find #"gogen" out-text)
                  (str "\nHint: the lg binary may be older than the"
                       " let-go checkout; run: make lg-dev")))))
    (doseq [line (str/split-lines (str (:out result)))]
      (when (str/includes? line "EMIT-FAIL")
        (die "lg-compile: " line)))
    (mapv str (fs/glob out-dir "**/*.go"))))

(defn LG-entry-code [entry-fn]
  (if entry-fn
    (str "\tif _, err := prog." entry-fn "(vm.RootExecContext, argv...);"
         " err != nil {\n\t\tfail(err)\n\t}")
    (str "\tf := vm.NewFrame(unit.MainChunk, nil)\n"
         "\t_, err = f.RunProtected()\n"
         "\tvm.ReleaseFrame(f)\n"
         "\tif err != nil {\n\t\tfail(err)\n\t}")))

(defn convert-file-LG-bin
  "Compile input to a native binary via let-go's AOT Go lowering.
  The program ns is lowered to a Go package, the whole program is
  bundled to bytecode with lg -c, and a generated main.go executes
  the bundle with the lowered fns wired in. When -main lowered
  (gloat's generated -main always does today), entry is a direct
  native call, so the program-ns call graph runs as plain Go."
  [input output namespace module]
  (let [input-type (get-file-type input)
        tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        src-dir (str tmpdir "/src")
        build-dir (str tmpdir "/build")
        lowered-dir (str build-dir "/lowered")
        clj-file (str tmpdir "/temp.clj")
        driver-file (str tmpdir "/driver.lg")
        ns (when (= input-type "ys")
             (or namespace (derive-namespace input)))]
    (try
      (case input-type
        "ys" (do
               (msg "Converting" input "(.ys) to Clojure...")
               (ys-to-clj input clj-file ns))
        "clj" (fs/copy input clj-file {:replace-existing true})
        (die "Engine 'let-go-lower-vm' can't compile input type: " input-type))

      (let [body (LG-rewrite-main (generate-lg-body clj-file))
            prog-ns (LG-program-ns body)
            ns-path (LG-ns-path prog-ns)
            pkg (last (str/split ns-path #"/"))
            lg-file (str src-dir "/" ns-path ".lg")
            go-module (or module
                          (str "github.com/gloathub/"
                               (fs/file-name output)))
            lg (find-lg)
            let-go-src (find-let-go-src)
            go-bin (:GO make-vars)]
        (fs/create-dirs (fs/parent lg-file))
        (fs/create-dirs lowered-dir)
        (spit lg-file body)
        ;; The driver requires the program as a namespace (so its
        ;; defns exist before the lowered overrides drain) and only
        ;; calls -main on the VM in the fallback-entry case.
        (spit driver-file
              (str "(require '" prog-ns ")\n"
                   "(when-not *compiling-aot*\n"
                   "  (when-let [main (resolve '" prog-ns "/-main)]\n"
                   "    (apply main *command-line-args*)))\n"))

        (msg "Lowering to native Go with lg-compile...")
        (let [emitted (lower-lg-to-go lg-file src-dir lowered-dir
                                      go-module)
              prog-go (str lowered-dir "/" ns-path "/" pkg ".go")
              ;; Direct native entry targets the fn lowered from
              ;; gloat's -main wrapper, identified by its exact
              ;; variadic signature. Its Go name is Main unless the
              ;; program defines its own main fn, which then owns
              ;; Main and pushes -main to Main__main.
              entry-fn (and (fs/exists? prog-go)
                            (second
                             (re-find
                              #"(?m)^func (Main(?:__main)?)\(ec \*vm\.ExecContext, args \.\.\.vm\.Value\)"
                              (slurp prog-go))))]
          (when (empty? emitted)
            (msg "Note: no defns lowered; binary runs pure bytecode"))

          (msg "Bundling bytecode with lg -c...")
          (let [result (process/shell
                        {:out :string :err :string :continue true}
                        lg "-source-paths"
                        (str src-dir ":" GLOAT-ROOT "/ys/lg")
                        "-c" (str build-dir "/program.lgb") driver-file)]
            (when-not (zero? (:exit result))
              (die (str "lg -c failed:\n"
                        (:out result) (:err result)))))

          ;; Render go.mod, then build. When the lowered Go fails to
          ;; compile (the lowering pipeline still has gaps), fall
          ;; back to a pure-bytecode binary, which matches -Elg
          ;; semantics, rather than failing the compile.
          (let [go-directive (or (second
                                  (re-find #"(?m)^go (\S+)"
                                           (slurp (str let-go-src
                                                       "/go.mod"))))
                                 "1.24")
                go-mod (-> (slurp (str TEMPLATE "/lg-go.mod"))
                           (str/replace "GO-MODULE" go-module)
                           (str/replace "GO-DIRECTIVE" go-directive)
                           (str/replace "LET-GO-SRC" let-go-src))
                main-go-for
                ;; mode :direct = native entry through -main;
                ;; :overrides = VM entry, lowered packages still
                ;; imported (their registered overrides apply);
                ;; :pure = bytecode only.
                (fn [mode]
                  (let [imports
                        (when-not (= :pure mode)
                          (for [f emitted
                                :let [dir (str (fs/parent f))
                                      rel (subs dir
                                                (inc (count
                                                      build-dir)))]]
                            (if (and (= :direct mode) (= f prog-go))
                              (str "\tprog \"" go-module "/" rel "\"")
                              (str "\t_ \"" go-module "/" rel "\""))))]
                    (-> (slurp (str TEMPLATE "/lg-main.go"))
                        (str/replace "LOWERED-IMPORTS"
                                     (str/join "\n" imports))
                        (str/replace "ENTRY-CODE"
                                     (LG-entry-code
                                      (when (= :direct mode)
                                        entry-fn))))))
                build-env (merge go-env {"GOTOOLCHAIN" "auto"
                                         "GOFLAGS" "-mod=mod"})
                out (str (fs/absolutize output))
                go-build
                (fn []
                  (let [tidy (process/shell
                              {:out :string :err :string
                               :continue true :dir build-dir
                               :extra-env build-env}
                              go-bin "mod" "tidy")]
                    (if-not (zero? (:exit tidy))
                      tidy
                      (process/shell {:out :string :err :string
                                      :continue true :dir build-dir
                                      :extra-env build-env}
                                     go-bin "build"
                                     "-trimpath"
                                     "-ldflags" "-s -w"
                                     "-o" out "."))))]
            (spit (str build-dir "/go.mod") go-mod)
            ;; Try the richest mode first, degrading on compile
            ;; failure: :direct (native -main entry) -> :overrides
            ;; (VM entry, lowered code still active) -> :pure
            ;; (bytecode only, matches -Elg). Warnings go to stderr
            ;; unconditionally (--run implies quiet) so the user
            ;; knows when a binary runs below full native speed.
            (loop [[mode & more] (concat
                                  (when entry-fn [:direct])
                                  (when (seq emitted) [:overrides])
                                  [:pure])]
              (spit (str build-dir "/main.go") (main-go-for mode))
              (when (= mode (if entry-fn :direct
                                (if (seq emitted) :overrides :pure)))
                (msg "Building native binary with go..."))
              (let [build (go-build)]
                (cond
                  (zero? (:exit build))
                  (msg "Generated:" output)

                  (empty? more)
                  (die (str "go build failed:\n"
                            (:out build) (:err build)))

                  :else
                  (do
                    (binding [*out* *err*]
                      (println "Warning: LG" (name mode)
                               "build failed; retrying as"
                               (name (first more)))
                      (println (str/trim (str (:out build)
                                              (:err build)))))
                    (recur more))))))))
      (finally
        (fs/delete-tree tmpdir)))))

(defn generate-LG-lowered-go
  "Lower input's program ns and return the emitted Go source text
  (the -t LG format). Dies when nothing lowered."
  [input namespace]
  (let [input-type (get-file-type input)
        tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        src-dir (str tmpdir "/src")
        lowered-dir (str tmpdir "/lowered")
        clj-file (str tmpdir "/temp.clj")
        ns (when (= input-type "ys")
             (or namespace (derive-namespace input)))]
    (try
      (case input-type
        "ys" (ys-to-clj input clj-file ns)
        "clj" (fs/copy input clj-file {:replace-existing true})
        (die "Engine 'let-go-lower-vm' can't compile input type: " input-type))
      (let [body (LG-rewrite-main (generate-lg-body clj-file))
            prog-ns (LG-program-ns body)
            ns-path (LG-ns-path prog-ns)
            pkg (last (str/split ns-path #"/"))
            lg-file (str src-dir "/" ns-path ".lg")]
        (fs/create-dirs (fs/parent lg-file))
        (fs/create-dirs lowered-dir)
        (spit lg-file body)
        (lower-lg-to-go lg-file src-dir lowered-dir "gloatbuild")
        (let [prog-go (str lowered-dir "/" ns-path "/" pkg ".go")]
          (when-not (fs/exists? prog-go)
            (die "-t LG: no lowerable defns in " input))
          (slurp prog-go)))
      (finally
        (fs/delete-tree tmpdir)))))

(defn convert-files-lg-lib
  "Build a c-shared library backed by a let-go bytecode bundle.
  In lower-vm mode, import the emitted Go packages so their native
  overrides replace eligible vars after each namespace loads."
  [input-files output namespace module platform lower?]
  (let [tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        src-dir (str tmpdir "/src")
        build-dir (str tmpdir "/build")
        lowered-dir (str build-dir "/lowered")
        driver-file (str tmpdir "/driver.lg")
        export-map (atom nil)
        export-ns (atom nil)
        all-nses (atom [])
        lg-files (atom [])
        has-main (atom false)]
    (try
      (fs/create-dirs src-dir)
      (fs/create-dirs build-dir)
      (fs/create-dirs lowered-dir)

      (doseq [[idx input] (map-indexed vector input-files)]
        (let [input (str input)
              input-type (get-file-type input)
              clj-file (str tmpdir "/input-" idx ".clj")
              ns (when (= input-type "ys")
                   (or namespace (derive-namespace input)))]
          (case input-type
            "ys" (do
                   (msg "Converting" input "(.ys) to Clojure...")
                   (ys-to-clj input clj-file ns))
            "clj" (fs/copy input clj-file {:replace-existing true})
            (die "let-go engines can't compile input type: " input-type))

          (let [clj-content (slurp clj-file)
                body (generate-lg-body clj-file)
                prog-ns (LG-program-ns body)
                ns-path (LG-ns-path prog-ns)
                lg-file (str src-dir "/" ns-path ".lg")]
            (when-let [exports (extract-export clj-content)]
              (when @export-map
                (die "Library input has more than one EXPORT declaration"))
              (reset! export-map exports)
              (reset! export-ns prog-ns))
            (when (has-main-fn? clj-content)
              (reset! has-main true))
            (fs/create-dirs (fs/parent lg-file))
            (spit lg-file body)
            (swap! all-nses conj prog-ns)
            (swap! lg-files conj lg-file))))

      (when-not @export-map
        (die "Library format requires EXPORT declaration.\n"
             "Add (def EXPORT {...}) with exported function signatures."))
      (when @has-main
        (die "Library format cannot have a main function.\n"
             "Libraries use EXPORT declaration, binaries use main."))

      ;; Compiling a driver which requires every user namespace puts the
      ;; namespace chunks and their transitive dependencies into one bundle.
      (spit driver-file
            (str/join "\n" (map #(str "(require '" % ")") @all-nses)))

      (let [let-go-src (find-let-go-src)
            lg (find-LG-lg let-go-src)
            go-bin (:GO make-vars)
            go-module (or module
                          (System/getenv "GLOAT_MODULE")
                          (str "github.com/gloathub/"
                               (fs/file-name output)))
            emitted (if lower?
                      (do
                        (msg "Lowering library to native Go with lg-compile...")
                        (doseq [lg-file @lg-files]
                          (lower-lg-to-go lg-file src-dir lowered-dir
                                          go-module))
                        (mapv str (fs/glob lowered-dir "**/*.go")))
                      [])
            _ (when (and lower? (empty? emitted))
                (msg "Note: no defns lowered; library runs pure bytecode"))]

        (msg "Bundling library bytecode with lg -c...")
        (let [result (process/shell
                      {:out :string :err :string :continue true}
                      lg "-source-paths"
                      (str src-dir ":" GLOAT-ROOT "/ys/lg")
                      "-c" (str build-dir "/program.lgb") driver-file)]
          (when-not (zero? (:exit result))
            (die (str "lg -c failed:\n" (:out result) (:err result)))))

        (let [go-directive (or (second
                                (re-find #"(?m)^go (\S+)"
                                         (slurp (str let-go-src "/go.mod"))))
                               "1.24")
              go-mod (-> (slurp (str TEMPLATE "/lg-go.mod"))
                         (str/replace "GO-MODULE" go-module)
                         (str/replace "GO-DIRECTIVE" go-directive)
                         (str/replace "LET-GO-SRC" let-go-src))
              lowered-imports
              (->> emitted
                   (map fs/parent)
                   distinct
                   (map (fn [dir]
                          (let [rel (subs (str dir)
                                          (inc (count build-dir)))]
                            (str "\t_ \"" go-module "/" rel "\""))))
                   (str/join "\n"))
              main-go-for
              (fn [mode]
                (-> (slurp (str TEMPLATE "/lg-lib-main.go"))
                    (str/replace "LOWERED-IMPORTS"
                                 (if (= mode :overrides)
                                   lowered-imports
                                   ""))
                    (str/replace "NAMESPACE" @export-ns)
                    (str/replace "EXPORT-FUNCTIONS"
                                 (generate-lg-export-functions @export-map))))
              [goos goarch] (if platform
                              (str/split platform #"/")
                              [nil nil])
              build-env
              (merge go-env
                     {"CGO_ENABLED" "1"
                      "GONOSUMCHECK" "*"
                      "GOTOOLCHAIN" "auto"
                      "GOFLAGS" "-mod=mod"}
                     (when (and (= goos "windows")
                                (not (System/getenv "CC")))
                       (let [cc "x86_64-w64-mingw32-gcc"]
                         (when-not (fs/which cc)
                           (die "Windows lib cross-compile needs mingw-w64 ("
                                cc "); install gcc-mingw-w64-x86-64 or set CC"))
                         {"CC" cc}))
                     (when goos {"GOOS" goos})
                     (when goarch {"GOARCH" goarch}))
              out (str (fs/absolutize output))
              go-build
              (fn []
                (let [tidy (process/shell
                            {:out :string :err :string
                             :continue true :dir build-dir
                             :extra-env build-env}
                            go-bin "mod" "tidy")]
                  (if-not (zero? (:exit tidy))
                    tidy
                    (process/shell
                     {:out :string :err :string
                      :continue true :dir build-dir
                      :extra-env build-env}
                     go-bin "build" "-trimpath" "-ldflags" "-s -w"
                     "-buildmode=c-shared" "-o" out "main.go"))))]
          (spit (str build-dir "/go.mod") go-mod)
          (msg "Building lib...")
          (timer-start)
          (loop [[mode & more] (if (and lower? (seq emitted))
                                [:overrides :pure]
                                [:pure])]
            (spit (str build-dir "/main.go") (main-go-for mode))
            (let [build (go-build)]
              (cond
                (zero? (:exit build))
                (do
                  (timer-end "GO→LIB")
                  (msg "Generated:" output)
                  (let [header (str
                                (str/replace out #"\.(so|dylib|dll)$" "")
                                ".h")]
                    (when (fs/exists? header)
                      (msg "Generated:" header))))

                (empty? more)
                (die (str "go build failed:\n"
                          (:out build) (:err build)))

                :else
                (do
                  (binding [*out* *err*]
                    (println "Warning: LG overrides build failed;"
                             "retrying as pure bytecode")
                    (println (str/trim (str (:out build) (:err build)))))
                  (recur more)))))))
      (finally
        (fs/delete-tree tmpdir)))))

;;------------------------------------------------------------------------------
;; GraalVM Engine
;;------------------------------------------------------------------------------

(defn find-managed-tool
  "Return a managed executable, installing it through a focused Make target
  when it is not present yet."
  [tool-key make-target label]
  (let [tool (get make-vars tool-key)]
    (if (and tool (fs/executable? tool))
      tool
      (let [result (process/shell {:out :string :err :string
                                   :continue true
                                   :dir GLOAT-ROOT}
                                  "make" "--quiet" "--no-print-directory"
                                  make-target)
            path (->> (str/split-lines (str (:out result)))
                      (remove str/blank?)
                      last)]
        (when-not (and (zero? (:exit result))
                       path (fs/executable? path))
          (die (str "Failed to install " label ":\n"
                    (:out result) (:err result))))
        path))))

(defn graal-main-function
  "Return the CLI entry function defined by a Clojure source string."
  [content]
  (cond
    (re-find #"\(defn\s+-main\b" content) "-main"
    (re-find #"\(defn\s+main\b" content) "main"
    :else nil))

(defn graal-ns-path [namespace]
  (-> namespace
      (str/replace "." "/")
      (str/replace "-" "_")))

(defn convert-files-graal-bin
  "AOT compile namespaced Clojure sources to an uberjar, then build a
  host-native executable with GraalVM Native Image."
  [input-files output namespace]
  (let [tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        src-dir (str tmpdir "/src")
        wrapper-dir (str src-dir "/gloat/graal")
        seen-nses (atom #{})
        entries (atom [])]
    (try
      (fs/create-dirs wrapper-dir)

      (doseq [input input-files]
        (let [input (str input)
              input-type (get-file-type input)]
          (when-not (= "clj" input-type)
            (die "Engine 'graalvm' only supports Clojure (.clj) input"
                 " (got " input ")"))
          (let [content (slurp input)
                source-ns (parse-namespace input)]
            (when-not source-ns
              (die "Engine 'graalvm' requires every source file to declare"
                   " an (ns ...) form: " input))
            (when (contains? @seen-nses source-ns)
              (die "Engine 'graalvm' received duplicate namespace: "
                   source-ns))
            (swap! seen-nses conj source-ns)
            (when-let [main-fn (graal-main-function content)]
              (swap! entries conj {:namespace source-ns
                                   :function main-fn
                                   :input input}))
            (let [target (str src-dir "/" (graal-ns-path source-ns) ".clj")]
              (fs/create-dirs (fs/parent target))
              (fs/copy input target {:replace-existing true})))))

      (when (empty? @entries)
        (die "Engine 'graalvm' requires a (defn -main ...) or"
             " (defn main ...) entry point"))

      (let [{entry-ns :namespace entry-fn :function} (first @entries)]
        (when (and namespace (not= namespace entry-ns))
          (die "--ns=" namespace " does not match GraalVM entry namespace "
               entry-ns))

        (spit (str wrapper-dir "/main.clj")
              (-> (slurp (str TEMPLATE "/graal-main.clj"))
                  (str/replace "ENTRY-NAMESPACE" entry-ns)
                  (str/replace "ENTRY-FUNCTION" entry-fn)))
        (spit (str tmpdir "/project.clj")
              (-> (slurp (str TEMPLATE "/graal-project.clj"))
                  (str/replace "CLOJURE-VERSION"
                               (:GRAAL-CLOJURE-VERSION make-vars))))

        (let [native-image (find-managed-tool
                            :GRAALVM "path-graalvm" "GraalVM")
              lein (find-managed-tool :LEIN "path-lein" "Leiningen")
              graal-home (str (fs/parent (fs/parent native-image)))
              local-home (:LOCAL-HOME make-vars)
              build-env {"JAVA_HOME" graal-home
                         "LEIN_HOME" local-home
                         "LEIN_JVM_OPTS"
                         (str "-Duser.home=" local-home
                              " -Dclojure.compiler.direct-linking=true"
                              " -Dclojure.spec.skip-macros=true")}
              io-opts (if (:quiet *opts*)
                        {:out :string :err :string}
                        {:out :inherit :err :inherit})]
          (msg "Compiling Clojure classes with Leiningen...")
          (timer-start)
          (let [result (process/shell
                        (merge {:continue true :dir tmpdir
                                :extra-env build-env}
                               io-opts)
                        lein "uberjar")]
            (when-not (zero? (:exit result))
              (die (str "lein uberjar failed"
                        (when (:quiet *opts*)
                          (str ":\n" (:out result) (:err result)))))))
          (timer-end "CLJ→JAR")

          (let [jar (str tmpdir "/target/gloat-graal.jar")
                out (str (fs/absolutize output))]
            (when-not (fs/exists? jar)
              (die "lein uberjar did not produce " jar))
            (msg "Building native binary with GraalVM...")
            (timer-start)
            (let [result
                  (apply process/shell
                         (merge {:continue true :dir tmpdir
                                 :extra-env build-env}
                                io-opts)
                         [native-image
                          "-O1"
                          "--no-fallback"
                          "--initialize-at-build-time"
                          "--initialize-at-run-time=clojure.lang.Compiler"
                          "-march=compatibility"
                          "-H:+ReportExceptionStackTraces"
                          "-J-Dclojure.spec.skip-macros=true"
                          "-J-Dclojure.compiler.direct-linking=true"
                          "-J-Xmx3g"
                          "-jar" jar
                          "-o" out])]
              (when-not (zero? (:exit result))
                (die (str "native-image failed"
                          (when (:quiet *opts*)
                            (str ":\n" (:out result) (:err result)))))))
            (timer-end "JAR→BIN")
            (when-not (fs/exists? out)
              (die "native-image did not produce " out))
            (msg "Generated:" output))))
      (finally
        (fs/delete-tree tmpdir)))))

;;------------------------------------------------------------------------------
;; Jolt Engine
;;------------------------------------------------------------------------------

(defn jolt-main? [content]
  (boolean (re-find #"\(defn\s+-main\b" content)))

(defn convert-files-jolt-bin
  "Build namespaced Clojure sources as a host-native executable with Jolt.
  PROJECT-ROOT supplies deps.edn; staged command-line sources take precedence
  over the project's ordinary source roots."
  [input-files output namespace project-root]
  (let [tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        src-dir (str tmpdir "/src")
        build-dir (str tmpdir "/build")
        build-out (str build-dir "/" (fs/file-name output))
        seen-nses (atom #{})
        entries (atom [])]
    (try
      (fs/create-dirs src-dir)
      (fs/create-dirs build-dir)

      (doseq [input input-files]
        (let [input (str input)
              input-type (get-file-type input)]
          (when-not (= "clj" input-type)
            (die "Engine 'jolt' only supports Clojure (.clj) input"
                 " (got " input ")"))
          (let [content (slurp input)
                source-ns (parse-namespace input)]
            (when-not source-ns
              (die "Engine 'jolt' requires every source file to declare"
                   " an (ns ...) form: " input))
            (when (contains? @seen-nses source-ns)
              (die "Engine 'jolt' received duplicate namespace: " source-ns))
            (swap! seen-nses conj source-ns)
            (when (jolt-main? content)
              (swap! entries conj {:namespace source-ns :input input}))
            (let [target (str src-dir "/" (graal-ns-path source-ns) ".clj")]
              (fs/create-dirs (fs/parent target))
              (fs/copy input target {:replace-existing true})))))

      (let [entry
            (if namespace
              (or (some #(when (= namespace (:namespace %)) %) @entries)
                  (if (contains? @seen-nses namespace)
                    (die "Engine 'jolt' entry namespace " namespace
                         " does not define -main")
                    (die "--ns=" namespace
                         " does not match a Jolt input namespace")))
              (case (count @entries)
                0 (die "Engine 'jolt' requires a (defn -main ...) entry point")
                1 (first @entries)
                (die "Engine 'jolt' received multiple -main namespaces: "
                     (str/join ", " (map :namespace @entries))
                     " (select one with --ns)")))
            entry-ns (:namespace entry)
            project-root (str (fs/canonicalize project-root))
            source-alias (pr-str
                           {:aliases
                            {:gloat/jolt-engine
                             {:extra-paths [src-dir]}}})
            jolt (find-managed-tool :JOLT "path-jolt" "Jolt")
            local-home (:LOCAL-HOME make-vars)
            jolt-cache (str local-home "/jolt/aot-cache")
            jolt-runtime-cache (str local-home "/jolt/runtime-cache")
            out (str (fs/absolutize output))
            args (cond-> [jolt
                          "-Sdeps" source-alias
                          "-A:gloat/jolt-engine"
                          "build" "-m" entry-ns "-o" build-out]
                   (prune?) (conj "--tree-shake"))
            io-opts (if (:quiet *opts*)
                      {:out :string :err :string}
                      {:out :inherit :err :inherit})]
        (msg "Building native binary with Jolt...")
        (timer-start)
        (let [result (apply process/shell
                            (merge {:continue true
                                    :extra-env
                                    {"JOLT_PWD" project-root
                                     "JOLT_CACHE_DIR" jolt-cache
                                     "JOLT_RUNTIME_CACHE_DIR"
                                     jolt-runtime-cache}}
                                   io-opts)
                            args)]
          (when-not (zero? (:exit result))
            (die (str "jolt build failed"
                      (when (:quiet *opts*)
                        (str ":\n" (:out result) (:err result)))))))
        (timer-end "CLJ→BIN")
        (when-not (fs/exists? build-out)
          (die "jolt build did not produce " build-out))
        (fs/copy build-out out
                 {:replace-existing true :copy-attributes true})
        (msg "Generated:" output))
      (finally
        (fs/delete-tree tmpdir)))))

;;------------------------------------------------------------------------------
;; High-Level Orchestrators
;;------------------------------------------------------------------------------

(defn convert-to-stdout [input format namespace]
  (let [;; Materialize stdin into a temp file when input is "-"
        [input stdin?]
        (if (= input "-")
          (let [content (slurp *in*)
                clj? (re-find #"^\s*\(" content)
                suffix (if clj? ".clj" ".ys")
                content (if (and clj? (not (re-find #"(?m)^\s*\(ns\s" content)))
                          (str "(ns main.core)\n" content)
                          content)
                tmpfile (str (fs/create-temp-file
                               {:dir GLOAT-TMP :suffix suffix}))]
            (spit tmpfile content)
            [tmpfile true])
          [input false])
        tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
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
        "lg" (print (generate-lg clj-file))
        "LG" (print (generate-LG-lowered-go input namespace))
        "glj" (do
                (when (fs/exists? clj-file)
                  (clj-to-glj clj-file glj-file))
                (print (slurp glj-file)))
        "go" (let [go-tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))]
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
        (fs/delete-tree tmpdir)
        (when stdin? (fs/delete input))))))

(defn convert-file-lg-bin
  "Compile input to a standalone binary by bundling its bytecode with
  the lg runtime (lg -b). No Go toolchain involved."
  [input output namespace]
  (let [input-type (get-file-type input)
        tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
        clj-file (str tmpdir "/temp.clj")
        lg-file (str tmpdir "/temp.lg")
        ns (when (= input-type "ys")
             (or namespace (derive-namespace input)))]
    (try
      (case input-type
        "ys" (do
               (msg "Converting" input "(.ys) to Clojure...")
               (ys-to-clj input clj-file ns))
        "clj" (fs/copy input clj-file {:replace-existing true})
        (die "Engine 'let-go-vm' can't compile input type: " input-type))
      (spit lg-file (generate-lg clj-file))
      (msg "Bundling binary with lg...")
      (let [lg (find-lg)
            out (str (fs/absolutize output))
            result (process/shell {:continue true
                                   :out :string :err :string}
                                  lg "-source-paths" lg-source-paths
                                  "-b" out lg-file)]
        (when-not (zero? (:exit result))
          (die (str "lg -b failed:\n" (:out result) (:err result))))
        (msg "Generated:" output))
      (finally
        (fs/delete-tree tmpdir)))))

(defn convert-file [input output format namespace module platform]
  (let [input-type (get-file-type input)]

    ;; lg engine binaries bundle bytecode; no Go build directory
    (if (and (= "let-go-vm" (:engine *opts*)) (= format "bin"))
      (do
        (when platform
          (die "Engine 'let-go-vm' does not support --platform yet"))
        (convert-file-lg-bin input output namespace))

    ;; LG engine binaries build lowered Go in their own temp module
    (if (and (= "let-go-lower-vm" (:engine *opts*)) (= format "bin"))
      (do
        (when platform
          (die "Engine 'let-go-lower-vm' does not support --platform yet"))
        (convert-file-LG-bin input output namespace module))

    ;; For formats that need directory build, delegate
    (if (contains? #{"dir" "bin" "lib" "wasm" "js"} format)
      (let [original-source (str (fs/canonicalize input))
            tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
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
      (let [tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
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
            "lg" (do
                   (spit output (generate-lg clj-file))
                   (msg "Generated:" output))
            "LG" (do
                   (spit output (generate-LG-lowered-go input namespace))
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
            (fs/delete-tree tmpdir)))))))))

(defn convert-files [input-files output format namespace module platform]
  "Compile multiple explicit input files to a binary/lib/dir output.
  Each file is copied to a temp directory with a unique name based on its
  namespace to avoid basename collisions (e.g. parser.clj at different depths).
  For lib format, the file with EXPORT is named 'main.clj' so that
  convert-directory selects it as the main namespace."
  (let [tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))]
    (try
      ;; Select the explicit entrypoint file so convert-directory stages it as
      ;; main and uses its namespace for the generated binary or library.
      (let [export-file (when (= format "lib")
                          (first (filter
                                  #(not-empty
                                    (or (extract-export (slurp (str %))) []))
                                  input-files)))
            main-file (when (not= format "lib")
                        (first (filter
                                #(has-main-fn? (slurp (str %)))
                                input-files)))
            entry-file (or export-file main-file)]
        (doseq [[idx source-file] (map-indexed vector input-files)]
          (let [source-file (str source-file)
                basename (fs/file-name source-file)
                input-type (get-file-type source-file)
                file-ns (when (contains? #{"clj" "glj"} input-type)
                          (parse-namespace source-file))
                ;; Name the entrypoint file 'main' so convert-directory picks
                ;; its namespace; use namespace-derived names for other files
                ;; to avoid basename collisions at different paths.
                unique-name (cond
                              (= source-file (str entry-file))
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
  (let [deps-file (resolve-deps-file)
        extra-deps (when deps-file (parse-gljdeps deps-file))
        is-dir-output (= format "dir")
        is-binary (contains? #{"bin" "lib" "wasm" "js"} format)
        output-dir (cond
                     is-dir-output (str/replace output #"/$" "")
                     is-binary (str (fs/create-temp-dir {:dir GLOAT-TMP}) "/build")
                     :else (str/replace output #"/$" ""))
        binary-name (when is-binary (fs/file-name output))
        build-mode (when (= format "lib") "-buildmode=c-shared")
        [goos goarch] (cond
                        (= format "wasm") ["wasip1" "wasm"]
                        (= format "js") ["js" "wasm"]
                        platform (str/split platform #"/")
                        :else [nil nil])
        binary-name
        (if (and (= format "lib")
                 (not (str/ends-with? output ".dylib"))
                 (not (str/ends-with? output ".so"))
                 (not (str/ends-with? output ".dll")))
          ;; Default lib extension follows the target OS
          (str binary-name (if (= goos "windows") ".dll" ".so"))
          binary-name)]

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

      (let [shared-tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))
            all-namespaces (atom [])
            main-namespace (atom nil)
            export-map (atom nil)
            has-main (atom false)
            required-nses (atom (set (ys-ns-order)))]

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

          ;; Copy the patched portable runtime into the writable compiler
          ;; workspace. Generated runtime loaders are excluded below because
          ;; the final module links github.com/gloathub/ys-v0-go.
          (let [ys-source-dir (ys-v0-source-dir)]
            (doseq [file (fs/glob ys-source-dir "**/*")]
              (when (fs/regular-file? file)
                (let [rel-path (str (fs/relativize ys-source-dir file))
                      target (str shared-tmpdir "/" rel-path)]
                  (fs/create-dirs (fs/parent target))
                  (fs/copy file target {:replace-existing true})))))

          ;; Make gljdeps.edn visible to glj compile in its CWD so it can
          ;; resolve third-party Go package call sites. glj invokes
          ;; `go get` for declared deps and compiles a small wrapper
          ;; against glojure, so the workspace needs a go.mod with the
          ;; same glojure replace/fork pinning prepare-repl-env writes.
          (when deps-file
            (fs/copy deps-file (str shared-tmpdir "/gljdeps.edn")
                     {:replace-existing true})
            (write-glj-workspace-mod
              shared-tmpdir
              (:GLOJURE-DIR make-vars)
              (:GLOJURE-VERSION make-vars)
              extra-deps))

          (let [glj (:GLJ make-vars)
                ;; glj genpkg (triggered by extra Go deps) reads GOARCH at
                ;; runtime; mirror prepare-repl-env which does the same.
                host-goarch (-> (process/shell {:out :string}
                                               (:GO make-vars) "env" "GOARCH")
                                :out
                                str/trim)
                compile-env (cond-> go-env
                              (seq extra-deps)
                              (assoc "GOARCH" host-goarch
                                     "GOFLAGS" "-mod=mod"))
                compile-env (prepend-glj-classpath compile-env shared-tmpdir)]
            ;; When deps are present the workspace go.mod we wrote needs
            ;; a tidy pass so the user's deps appear before glj compiles.
            (when (seq extra-deps)
              (let [tidy (process/shell
                           {:dir shared-tmpdir
                            :extra-env compile-env
                            :out :string :err :string
                            :continue true}
                           (:GO make-vars) "mod" "tidy")]
                (when-not (zero? (:exit tidy))
                  (die "go mod tidy failed in glj workspace:\n"
                       (or (not-empty (:err tidy)) (:out tidy))))))
            ;; Compile all user namespaces
            (doseq [ns @all-namespaces]
              (msg "  Compiling" ns "...")
              (let [compile-cmd (str "(compile (quote " ns "))")
                    opts {:in compile-cmd
                          :dir shared-tmpdir
                          :extra-env compile-env
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

          ;; Copy generated user Go files to output directory under pkg/.
          ;; Runtime loaders come from the external ys-v0-go module.
          (fs/create-dirs (str output-dir "/pkg"))
          (doseq [gofile (fs/glob shared-tmpdir "**/*.go")]
            (let [rel-path (str (fs/relativize shared-tmpdir gofile))
                  runtime-paths ["ys/" "babashka/" "clojure/data/"
                                 "clojure/walk/"]
                  is-runtime? (some #(str/starts-with? rel-path %)
                                    runtime-paths)]
              (when-not is-runtime?
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
                  glojure-dir (:GLOJURE-DIR make-vars)
                  ys-v0-go-version (:YS-V0-GO-VERSION make-vars)
                  ys-v0-go-dir (:YS-V0-GO-DIR make-vars)
                  template-content (slurp (str TEMPLATE "/go.mod"))
                  result (render-template
                          template-content
                          [["GO-MODULE" go-module]
                           ["GLOJURE-VERSION" (go-mod-version glojure-version)]
                           ["YS-V0-GO-VERSION" ys-v0-go-version]
                           ["GLOAT-ROOT" GLOAT-ROOT]
                           ["EXTRA-DEPS" (render-extra-deps extra-deps)]])
                  local-glojure-replace
                  (when (and (seq glojure-dir) (fs/exists? glojure-dir))
                    [["github.com/glojurelang/glojure" glojure-dir]])
                  local-ys-v0-go-replace
                  (when (and (seq ys-v0-go-dir) (fs/exists? ys-v0-go-dir))
                    [["github.com/gloathub/ys-v0-go" ys-v0-go-dir]])
                  replaces (concat local-glojure-replace
                                   local-ys-v0-go-replace)
                  replace-block (when (seq replaces)
                                  (str "\n"
                                       (str/join "\n"
                                                 (map (fn [[old new]]
                                                        (str "replace " old
                                                             " => " new))
                                                      replaces))
                                       "\n"))
                  result (str result (or replace-block ""))]
              (spit (str output-dir "/go.mod") result)
              (msg "Generated:" (str output-dir "/go.mod"))
              (when (seq extra-deps)
                (msg "Extra deps:"
                     (str/join ", "
                               (map (fn [[p v]] (str p "@" v))
                                    extra-deps)))))

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
                                     ;; Cross-compiling a lib needs a C cross
                                     ;; toolchain: for windows targets, use
                                     ;; mingw-w64 unless the user set CC.
                                     (when (and (= format "lib")
                                                (= goos "windows")
                                                (not (System/getenv "CC")))
                                       (let [cc "x86_64-w64-mingw32-gcc"]
                                         (when-not (fs/which cc)
                                           (die "Windows lib cross-compile"
                                                " needs mingw-w64 (" cc ");"
                                                " install gcc-mingw-w64-x86-64"
                                                " or set CC"))
                                         {"CC" cc}))
                                     (when goos {"GOOS" goos})
                                     (when goarch {"GOARCH" goarch})
                                     (when (and (= goos "plan9")
                                                (str/starts-with?
                                                  (str/trim
                                                    (:out (process/shell
                                                            {:out :string
                                                             :err :string}
                                                            go-bin "version")))
                                                  "go version go1.24"))
                                       {"GOEXPERIMENT" "nospinbitmutex"}))]

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
                  (let [build-tags
                        (str/join "," (aot-build-tags))
                        build-args
                        (concat [go-bin "build"
                                 "-trimpath"
                                 "-ldflags" "-s -w"
                                 "-o" binary-name]
                                (when (seq build-tags)
                                  ["-tags" build-tags])
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
                      (let [compress-exts
                            (filter #{"gzip" "brotli"}
                                    (keys (parse-extensions
                                           (or (:ext *opts*) []))))]
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
                        (let [strip-lib-ext
                              (fn [s] (str/replace
                                       s #"\.(so|dylib|dll)$" ""))
                              h-file (str (strip-lib-ext binary-name) ".h")
                              h-output (str (strip-lib-ext output) ".h")
                              h-source (str output-dir "/" h-file)]
                          (when (fs/exists? h-source)
                            (fs/copy h-source h-output {:replace-existing true})
                            (msg "Generated:" h-output))))

                      ;; Generate binary size report if requested
                      (when-let [{:keys [path keep open] report-fmt :format} (report-ext)]
                        (msg "Analyzing binary size...")
                        (let [output-name (fs/file-name output)
                              keep-path (str output "-unstripped")
                              unstripped-name "unstripped-report"
                              a-build-tags
                              (str/join "," (aot-build-tags))
                              unstripped-args
                              (concat [go-bin "build"
                                       "-trimpath"
                                       "-o" unstripped-name]
                                      (when (seq a-build-tags)
                                        ["-tags" a-build-tags])
                                      (when build-mode [build-mode])
                                      ["main.go"])]
                          (apply process/shell
                                 {:dir output-dir
                                  :extra-env build-env
                                  :out :string :err :string}
                                 unstripped-args)
                          (let [unstripped-file (str output-dir "/"
                                                     unstripped-name)]
                            ;; Copy unstripped next to output if keeping
                            (when keep
                              (fs/copy unstripped-file keep-path
                                       {:replace-existing true})
                              (msg "Kept unstripped:" keep-path))
                            (report/generate-report
                             {:stripped-binary (str output)
                              :unstripped-binary (if keep
                                                   keep-path
                                                   unstripped-file)
                              :output-name output-name
                              :goos goos
                              :goarch goarch
                              :go-bin go-bin
                              :gloat-version VERSION
                              :glojure-version (:GLOJURE-VERSION make-vars)
                              :pruned (prune?)
                              :report-path path
                              :report-format report-fmt
                              :sources (mapv (fn [f]
                                              {:name (str (fs/file-name f))
                                               :content (slurp (str f))})
                                            source-files)})
                            (msg "Generated:" path)
                            (when open
                              (open/open-browser
                               (str "file://" (fs/absolutize path))))
                            (when-not keep
                              (fs/delete unstripped-file)))))

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
                           (str "&& go build -tags "
                                (str/join "," (aot-build-tags))))
                      (msg "To build: cd" output-dir "&& make")))
                  (do
                    (msg "Generated Go module in:" output-dir)
                    (msg "To build: cd" output-dir
                         (if (goimports?)
                           "&& go build -tags glj_aot_runtime"
                           "&& go build -tags glj_aot_runtime,glj_no_goimports"))))))))

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
        run (:run opts)
        engine (resolve-engine opts)
        format-guess (if (and (nil? output) (nil? to))
                       ;; --run under the lg engine defaults to lg format
                       (if (and run (= "let-go-vm" engine)) "lg" "bin")
                       (infer-format output
                                     (when to (str/replace to #"^\." ""))))
        ;; -t lg / -o foo.lg implies let-go-vm; -t LG let-go-lower-vm
        engine (cond
                 (= "lg" format-guess) "let-go-vm"
                 (= "LG" format-guess) "let-go-lower-vm"
                 :else engine)]

    (when (:width opts)
      (when-not (:fmt opts)
        (die "--width requires --fmt"))
      (when-not (re-matches #"[1-9][0-9]*" (:width opts))
        (die "--width must be a positive integer")))

    (when (and (= "let-go-vm" engine)
               (not (contains? lg-engine-formats format-guess)))
      (die "Engine 'let-go-vm' does not yet support format '" format-guess "'"
           " (supported: " (str/join ", " (sort lg-engine-formats)) ")"))

    (when (and (= "let-go-lower-vm" engine)
               (not (contains? LG-engine-formats format-guess)))
      (die "Engine 'let-go-lower-vm' does not yet support format '" format-guess "'"
           " (supported: " (str/join ", " (sort LG-engine-formats)) ")"))

    (when (and (= "graalvm" engine)
               (not (contains? graal-engine-formats format-guess)))
      (die "Engine 'graalvm' does not support format '" format-guess "'"
           " (supported: bin)"))

    (when (and (= "jolt" engine)
               (not (contains? jolt-engine-formats format-guess)))
      (die "Engine 'jolt' does not support format '" format-guess "'"
           " (supported: bin)"))

    (when (= "graalvm" engine)
      (when platform
        (die "Engine 'graalvm' does not support --platform;"
             " native-image builds for the host platform"))
      (when module
        (die "Engine 'graalvm' does not support --module"
             " (it is a Go module option)"))
      (when (seq (:ext opts))
        (die "Engine 'graalvm' does not support -X/--ext processing"
             " extensions"))
      (when (resolve-deps-file)
        (die "Engine 'graalvm' does not support gljdeps.edn;"
             " only self-contained Clojure sources are supported")))

    (when (= "jolt" engine)
      (when platform
        (die "Engine 'jolt' does not support --platform;"
             " jolt builds for the host platform"))
      (when module
        (die "Engine 'jolt' does not support --module"
             " (Jolt projects use deps.edn)"))
      (let [unsupported (remove #{"prune"}
                                (keys (parse-extensions
                                       (or (:ext opts) []))))]
        (when (seq unsupported)
          (die "Engine 'jolt' only supports -Xprune"
               " (unsupported: " (str/join ", " (sort unsupported)) ")")))
      (when (resolve-deps-file)
        (die "Engine 'jolt' does not support gljdeps.edn;"
             " use deps.edn for Jolt dependencies")))

    (when (and (:time opts) (not run))
      (die "--time requires --run"))

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
          (binding [*opts* (assoc opts :engine engine)]
            (cond
              (= engine "graalvm")
              (convert-files-graal-bin files output namespace)

              (= engine "jolt")
              (convert-files-jolt-bin
                files output namespace (fs/cwd))

              (and (= format "lib")
                   (contains? #{"let-go-vm" "let-go-lower-vm"} engine))
              (convert-files-lg-lib
                files output namespace module platform
                (= engine "let-go-lower-vm"))

              :else
              (convert-files
                files output format namespace module platform)))
          (System/exit 0)))

      ;; Validate input
      (let [input (or input "-")]

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
                    (let [run-tmpdir (str (fs/create-temp-dir {:dir GLOAT-TMP}))]
                      (alter-var-root #'*opts* assoc :run-tmpdir run-tmpdir)
                      (cond
                        (= to "bb") [(str run-tmpdir "/gloat-run.bb") to]
                        (and (nil? to) (= "let-go-vm" engine))
                        [(str run-tmpdir "/gloat-run.lg") "lg"]
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
                       :engine engine
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
      (do-engines)
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
            (if (contains? #{"clj" "bb" "lg" "LG" "glj" "go"} format)
              (convert-to-stdout
               (:input opts) format (or (:namespace opts) "main.core"))
              (die "Format '" format "' requires -o output"))

            (fs/regular-file? (:input opts))
            (cond
              (= (:engine opts) "graalvm")
              (convert-files-graal-bin
                [(:input opts)]
                (:output opts)
                (:namespace opts))

              (= (:engine opts) "jolt")
              (convert-files-jolt-bin
                [(:input opts)]
                (:output opts)
                (:namespace opts)
                (fs/cwd))

              (and (= format "lib")
                   (contains? #{"let-go-vm" "let-go-lower-vm"}
                              (:engine opts)))
              (convert-files-lg-lib
                [(:input opts)]
                (:output opts)
                (:namespace opts)
                (:module opts)
                (:platform opts)
                (= (:engine opts) "let-go-lower-vm"))

              :else
              (convert-file
                (:input opts)
                (:output opts)
                format
                (:namespace opts)
                (:module opts)
                (:platform opts)))

            (fs/directory? (:input opts))
            (cond
              (= (:engine opts) "graalvm")
              (convert-files-graal-bin
                (expand-dir-args [(:input opts)])
                (:output opts)
                (:namespace opts))

              (= (:engine opts) "jolt")
              (convert-files-jolt-bin
                (expand-dir-args [(:input opts)])
                (:output opts)
                (:namespace opts)
                (:input opts))

              (and (= format "lib")
                   (contains? #{"let-go-vm" "let-go-lower-vm"}
                              (:engine opts)))
              (convert-files-lg-lib
                (expand-dir-args [(:input opts)])
                (:output opts)
                (:namespace opts)
                (:module opts)
                (:platform opts)
                (= (:engine opts) "let-go-lower-vm"))

              :else
              (convert-directory
                (:input opts)
                (:output opts)
                format
                (:namespace opts)
                (:module opts)
                (:platform opts)))

            (= (:input opts) "-")
            (let [content (slurp *in*)
                  clj? (re-find #"^\s*\(" content)
                  suffix (if clj? ".clj" ".ys")
                  content (if (and clj? (not (re-find #"(?m)^\s*\(ns\s" content)))
                            (str "(ns main.core)\n" content)
                            content)
                  tmpfile (str (fs/create-temp-file {:dir GLOAT-TMP :suffix suffix}))]
              (spit tmpfile content)
              (cond
                (= (:engine opts) "graalvm")
                (convert-files-graal-bin
                  [tmpfile]
                  (:output opts)
                  (:namespace opts))

                (= (:engine opts) "jolt")
                (convert-files-jolt-bin
                  [tmpfile]
                  (:output opts)
                  (:namespace opts)
                  (fs/cwd))

                (and (= format "lib")
                     (contains? #{"let-go-vm" "let-go-lower-vm"}
                                (:engine opts)))
                (convert-files-lg-lib
                  [tmpfile]
                  (:output opts)
                  (:namespace opts)
                  (:module opts)
                  (:platform opts)
                  (= (:engine opts) "let-go-lower-vm"))

                :else
                (convert-file
                  tmpfile
                  (:output opts)
                  format
                  (:namespace opts)
                  (:module opts)
                  (:platform opts)))
              (fs/delete tmpfile))

            :else
            (die "Invalid input: " (:input opts)))

          ;; Execute compiled output if --run
          (when (:run opts)
            (when-not (fs/exists? (:output opts))
              (die "Compilation failed - no output to run"))

            ;; --time excludes tool lookup from the timed region
            (let [runner
                  (case format
                    "bb"
                    (let [bb (:BB make-vars)]
                      (when-not (fs/executable? bb)
                        (die (str
                              "Babashka not found"
                              "(run 'make shell' to install)")))
                      #(apply process/shell {:continue true}
                              bb (:output opts) (:run-args opts)))

                    "lg"
                    (let [lg (find-lg)]
                      #(apply process/shell {:continue true}
                              lg "-source-paths" lg-source-paths
                              (:output opts) (:run-args opts)))

                    ("bin" "lib" "wasm" "js")
                    #(apply process/shell {:continue true}
                            (:output opts) (:run-args opts))

                    (die "Format '" format
                         "' cannot be executed with --run"))
                  t0 (System/nanoTime)
                  rc (:exit (runner))
                  elapsed (- (System/nanoTime) t0)]

              (when (:time opts)
                (binding [*out* *err*]
                  (println (clojure.core/format
                            "> gloat run time: %.3fs"
                            (/ elapsed 1.0e9)))))
              (when (:run-tmpdir opts)
                (fs/delete-tree (:run-tmpdir opts)))
              (System/exit rc)))

          (print-verbose-footer opts))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
