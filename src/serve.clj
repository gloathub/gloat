;; serve.clj - Start a local HTTP server for WASM js builds

(ns serve
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]))

(defn serve
  "Start a local HTTP server to serve a WASM js build.

   Config keys:
     :output       - the .js output path
     :go-bin       - path to Go binary (to locate wasm_exec.js)
     :template-dir - path to template directory
     :program-args - vector of program arg strings, or []
     :quiet        - suppress output messages
     :has-html     - whether -Xhtml already generated HTML alongside output
     :open         - if true, open browser + use non-blocking server
     :gloat-root   - path to gloat project root (for make invocation)

   When :has-html is false, copies wasm to temp dir and calls html/generate
   there. When :open is true, starts server non-blocking, opens browser,
   then waits."
  [{:keys [output has-html open gloat-root] :as config}]
  (let [serve-dir (if has-html
                    (str (fs/parent (fs/absolutize output)))
                    (let [tmpdir (str (fs/create-temp-dir))]
                      (fs/copy output (str tmpdir "/" (fs/file-name output)))
                      (html/generate (assoc config
                                            :output (str tmpdir "/"
                                                         (fs/file-name output))
                                            :serve true))
                      tmpdir))
        html-name (str/replace (fs/file-name output) #"\.js$" ".html")
        url (str "http://localhost:8000/" html-name)
        make-args ["make" "--quiet" "--no-print-directory"
                   (str "SERVE-DIR=" serve-dir)
                   "python-local-server"]]
    (when-not (:quiet config)
      (binding [*out* *err*]
        (println (str "\nWhen server has started, open: " url "\n"))))
    (if open
      (let [proc (apply process/process {:dir gloat-root} make-args)]
        (Thread/sleep 500)
        (open/open-browser url)
        @proc)
      (apply process/shell {:dir gloat-root} make-args))))
