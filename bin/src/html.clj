;; html.clj - Generate HTML browser page for WASM js builds

(ns html
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]))

(defn generate
  "Generate an HTML page for running a WASM js module in the browser.

   Config keys:
     :output       - the .js output path
     :go-bin       - path to Go binary (to locate wasm_exec.js)
     :template-dir - path to template directory
     :program-args - vector of program arg strings, or []
     :quiet        - suppress output messages
     :serve        - true if -Xserve is active (suppress hint)

   Returns the html output path."
  [{:keys [output go-bin template-dir program-args quiet serve]}]
  (let [go-root (str (fs/parent (fs/parent go-bin)))
        wasm-exec-js (str go-root "/lib/wasm/wasm_exec.js")
        html-output (str/replace output #"\.js$" ".html")
        title (-> (fs/file-name output) (str/replace #"\.js$" ""))
        wasm-file (fs/file-name output)
        args-json (str "[" (str/join ", "
                                     (map #(str "\"" % "\"") program-args))
                       "]")
        template (slurp (str template-dir "/index.html"))
        wasm-exec-content (slurp wasm-exec-js)
        html (-> template
                 (str/replace "WASM-EXEC-JS" wasm-exec-content)
                 (str/replace "TITLE" title)
                 (str/replace "WASM-FILE" wasm-file)
                 (str/replace "PROGRAM-ARGS" args-json))]
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
