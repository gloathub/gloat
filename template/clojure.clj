(ns NAMESPACE
 (:require [ys.fs :as fs]
           [ys.ipc :as ipc]
           [ys.std :refer :all]
           [ys.dwim :refer :all]
           [ys.v0 :refer [map-parse]]))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])

BODY

(defn -main [& argv]
 (let [args (map-parse argv)]
 (alter-var-root #'ARGV (constantly argv))
 (alter-var-root #'ARGS (constantly args))
 (apply main args)))
