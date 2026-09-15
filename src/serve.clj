;; serve.clj - Start a local HTTP server for WASM js builds

(ns serve
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]))

(def port 8000)

(defn- url-encode [text]
  (-> (java.net.URLEncoder/encode text "UTF-8")
      (str/replace "+" "%20")))

(defn serve-location [output]
  (let [js-path (fs/absolutize output)
        html-name (-> (fs/file-name js-path)
                      (str/replace #"\.js$" ".html"))
        output-dir (fs/parent js-path)]
    (if (= html-name "index.html")
      (let [bundle-name (fs/file-name output-dir)]
        {:directory (str (fs/parent output-dir))
         :url (str "http://localhost:" port "/"
                   (url-encode bundle-name) "/index.html")})
      {:directory (str output-dir)
       :url (str "http://localhost:" port "/" (url-encode html-name))})))

(defn- port-open? []
  (try
    (with-open [socket (java.net.Socket.)]
      (.connect socket (java.net.InetSocketAddress. "127.0.0.1" port) 100)
      true)
    (catch Exception _ false)))

(defn- fail [message]
  (binding [*out* *err*]
    (println message)
    (flush))
  (System/exit 1))

(defn- wait-until-ready [server]
  (loop [tries 200]
    (cond
      (port-open?) true
      (not (process/alive? server))
      (fail "Local HTTP server stopped before becoming ready.")
      (zero? tries)
      (do
        (process/destroy-tree server)
        (fail "Timed out waiting for the local HTTP server."))
      :else
      (do
        (Thread/sleep 50)
        (recur (dec tries))))))

(defn serve
  "Start a local HTTP server and wait for it to exit."
  [{:keys [output open gloat-root quiet]}]
  (when (port-open?)
    (fail (str "Port " port " is already in use.")))
  (let [{:keys [directory url]} (serve-location output)
        make-args ["make" "--quiet" "--no-print-directory"
                   (str "SERVE-DIR=" directory)
                   "python-local-server"]
        server (apply process/process
                      {:dir gloat-root :out :discard :err :discard}
                      make-args)]
    (wait-until-ready server)
    (when-not quiet
      (binding [*out* *err*]
        (println (str "Now serving " url))
        (flush)))
    (when open
      (open/open-browser url))
    @server))
