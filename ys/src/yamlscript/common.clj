;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.common
  (:require
   [clojure.string :as str]
   #_[babashka.fs :refer [cwd]]
   #_[clojure.java.io :as io]
   #_[clojure.stacktrace]
   #_[yamlscript.debug]
   [yamlscript.util :as util]))

;; Use for error messages at some point
#_(defn find-var-by-value [x]
  (let [all-the-vars (mapcat (fn [ns]
                               (vals (ns-publics ns)))
                             (all-ns))]
    (first (filter (fn [var]
                     (identical? x @var)) all-the-vars))))
#_(time (prn (meta (find-var-by-value inc))))

#_(defn abspath
  ([path] (abspath path (str (cwd))))
  ([path base]
   (if (-> path io/file .isAbsolute)
     path
     (.getAbsolutePath (io/file (abspath base) path)))))

(defn atom? [x]
  (= (type x) clojure.lang.Atom))

(defn chop
  ([S] (chop 1 S))
  ([N S]
   (let [lst (drop-last N S)]
     (if (string? S)
       (str/join "" lst)
       lst))))

#_(defn dirname [path]
  (->
    path
    io/file
    .getParent
    (or ".")))

#_(defn get-process-handle []
  (java.lang.ProcessHandle/current))

#_(defn get-process-info []
  (-> ^java.lang.ProcessHandle (get-process-handle) .info))

#_(defn get-cmd-path []
  (-> ^java.lang.ProcessHandle$Info (get-process-info) .command .get))

#_(defn get-cmd-bin []
  (-> ^String (get-cmd-path) io/file .getParent))

#_(defn get-cmd-args []
  (-> ^java.lang.ProcessHandle$Info
      (get-process-info)
      .arguments
      (.orElse (into-array String []))))

#_(defn get-cmd-pid []
  (-> ^java.lang.ProcessHandle (get-process-handle) .pid))

#_(defn get-yspath [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #"/NO-NAME$" base) (str (cwd)))
                 (->
                   base
                   dirname
                   abspath))
        _ (when-not yspath
            (util/die "YSPATH environment variable not set"))]
    (str/split yspath #":")))

(defn re-find+ [R S]
  (re-find R (str S)))

(defn regex? [x]
  (= (type x) java.util.regex.Pattern))

#_(intern 'clojure.core (with-meta 'TTT {:macro true}) @#'yamlscript.debug/TTT)
#_(intern 'clojure.core 'YSC yamlscript.debug/YSC)
#_(intern 'clojure.core 'YSC0 yamlscript.debug/YSC0)
#_(intern 'clojure.core 'DBG yamlscript.debug/DBG)
#_(intern 'clojure.core 'PPP yamlscript.debug/PPP)
#_(intern 'clojure.core 'WWW yamlscript.debug/WWW)
#_(intern 'clojure.core 'XXX yamlscript.debug/XXX)
#_(intern 'clojure.core 'YYY yamlscript.debug/YYY)
#_(intern 'clojure.core 'ZZZ yamlscript.debug/ZZZ)

(comment
  )
