;; https://rosettacode.org/wiki/Palindrome_detection#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn palindrome? [s]
  (= s (str/reverse s)))

(defn -main [& args]
  (let [n (or (first args) "31337")
        is-palindrome (palindrome? n)
        not-str (if is-palindrome "" " not")]
    (println (str n " is" not-str " a palindrome."))))
