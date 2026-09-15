;; html.clj - Generate HTML browser page for WASM js builds

(ns html
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]))

(defn- html-escape [text]
  (-> text
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- javascript-string [text]
  (-> (pr-str text)
      (str/replace "<" "\\u003c")
      (str/replace ">" "\\u003e")
      (str/replace "&" "\\u0026")))

(defn program-name [output]
  (let [filename (fs/file-name output)
        stem (str/replace filename #"\.js$" "")]
    (if (= stem "index")
      (or (some-> output fs/absolutize fs/parent fs/file-name str)
          stem)
      stem)))

(defn output-path [output]
  (str/replace output #"\.js$" ".html"))

(defn generate
  "Generate an HTML page for running a WASM js module in the browser.

   Config keys:
     :output       - the .js output path
     :go-bin       - path to Go binary (to locate wasm_exec.js)
     :template-dir - path to template directory
     :quiet        - suppress output messages
     :serve        - true if -Xserve is active (suppress hint)

   Returns the html output path."
  [{:keys [output go-bin template-dir quiet serve]}]
  (let [go-root (str (fs/parent (fs/parent go-bin)))
        wasm-exec-js (str go-root "/lib/wasm/wasm_exec.js")
        html-output (output-path output)
        title (program-name output)
        wasm-file (fs/file-name output)
        template (slurp (str template-dir "/index.html"))
        wasm-exec-content (slurp wasm-exec-js)
        html (-> template
                 (str/replace "WASM-EXEC-JS" wasm-exec-content)
                 (str/replace "PROGRAM-NAME-JSON"
                              (javascript-string title))
                 (str/replace "PROGRAM-NAME" (html-escape title))
                 (str/replace "WASM-FILE" wasm-file))]
    (spit html-output html)
    (when-not quiet
      (binding [*out* *err*]
        (println (str "Generated: " html-output))))
    (when (and (not quiet) (not serve))
      (let [html-name (fs/file-name html-output)]
        (binding [*out* *err*]
          (println (str "\nTo run the Wasm module '" wasm-file "', run:\n"
                        "python3 -m http.server\n"
                        "\nAnd click:\n"
                        "http://localhost:8000/" html-name "\n")))))
    html-output))
