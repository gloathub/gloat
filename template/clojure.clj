(ns NAMESPACE
  (:require [ys.fs :as fs]
            [ys.http :as http]
            [ys.ipc :as ipc]
            [ys.json :as json]
            [ys.std :refer :all]
            [ys.dwim :refer :all]
            [ys.v0 :refer [map-parse]]))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])
(def ^:dynamic ENV {})
(def ^:dynamic NS nil)
(def ^:dynamic RUN {})
(def ^:dynamic FILE "SOURCE-FILE")
(def ^:dynamic DIR "SOURCE-DIR")
(def ^:dynamic CWD "")

BODY

(defn -main [& argv]
  (let [args (map-parse argv)]
    (alter-var-root #'ARGV (constantly argv))
    (alter-var-root #'ARGS (constantly args))
    (apply main args)))
  