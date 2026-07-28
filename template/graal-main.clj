(ns gloat.graal.main
  (:require [ENTRY-NAMESPACE :as entry])
  (:gen-class))

(defn -main [& args]
  (apply entry/ENTRY-FUNCTION args))
