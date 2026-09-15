;; https://rosettacode.org/wiki/Tokenize_a_string#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn tokenize [s delimiter]
  (str/split s (re-pattern delimiter)))

(defn -main [& args]
  (let [input (or (first args) "Hello,How,Are,You,Today")
        tokens (tokenize input ",")
        result (str/join "." tokens)]
    (println result)))
