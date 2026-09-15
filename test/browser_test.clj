(ns browser-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests]]))

(load-file "src/html.clj")
(load-file "src/open.clj")
(load-file "src/serve.clj")
(load-file "src/gloat.clj")

(deftest implicit-serve-output
  (let [opts (gloat/set-vars
              {:args ["test/fixtures/hello.ys"]
               :to "js"
               :ext ["serve"]})]
    (is (= "hello/index.js" (:output opts)))
    (is (= "js" (:to opts)))))

(deftest served-locations
  (let [root (str (fs/cwd))]
    (is (= {:directory root
            :url "http://localhost:8000/foo/index.html"}
           (serve/serve-location "foo/index.js")))
    (is (= {:directory (str root "/web")
            :url "http://localhost:8000/app.html"}
           (serve/serve-location "web/app.js")))
    (is (= "http://localhost:8000/with%20space/index.html"
           (:url (serve/serve-location "with space/index.js"))))))

(deftest server-announcement
  (let [checks (atom 0)
        output (java.io.StringWriter.)
        server (delay {:exit 0})]
    (with-redefs-fn
      {#'serve/port-open? #(= 2 (swap! checks inc))
       #'process/process (fn [& _] server)
       #'process/alive? (constantly true)}
      #(binding [*err* output]
         (serve/serve {:output "foo/index.js"
                       :gloat-root (str (fs/cwd))})))
    (is (= "Now serving http://localhost:8000/foo/index.html\n"
           (str output)))))

(deftest browser-page
  (let [dir (str (fs/create-temp-dir))
        go-bin (str dir "/go/bin/go")
        wasm-exec (str dir "/go/lib/wasm/wasm_exec.js")
        output (str dir "/beer/index.js")]
    (try
      (doseq [path [(fs/parent go-bin) (fs/parent wasm-exec)
                    (fs/parent output)]]
        (fs/create-dirs path))
      (spit wasm-exec
            "globalThis.fs = {writeSync(fd, buf) {return buf.length;}};\n")
      (html/generate {:output output
                      :go-bin go-bin
                      :template-dir "template"
                      :quiet true})
      (let [page (slurp (html/output-path output))]
        (is (str/includes? page "<title>beer</title>"))
        (is (str/includes? page "go.argv = [\"beer\"].concat(queryArgs())"))
        (is (str/includes? page "query.split(',').map(decodeURIComponent)"))
        (is (str/includes? page "globalThis.fs.writeSync = function"))
        (is (str/includes? page ".output-row.stderr { background: #fff0f0; }"))
        (is (not (str/includes? page "<h1>")))
        (is (not (str/includes? page "Done.")))
        (is (not (str/includes? page "console.log ="))))
      (finally
        (fs/delete-tree dir)))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (+ fail error)))
