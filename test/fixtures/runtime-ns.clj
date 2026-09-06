(ns runtime-ns
  (:require
   [clojure.string :as str]
   [ys.v0.json :as json]))

(defn -main [& args]
  (println (json/dump {:args (str/join " " args)})))
