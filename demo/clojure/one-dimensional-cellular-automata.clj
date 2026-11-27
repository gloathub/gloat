;; https://rosettacode.org/wiki/One-dimensional_cellular_automata#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn next-gen [cells]
  (loop [cs cells
         ncs (subs cells 0 1)]
    (if (>= (count cs) 3)
      (let [f3 (subs cs 0 3)
            new-cell (if (= 2 (count (filter #(= \# %) f3))) "#" "_")]
        (recur (subs cs 1)
               (str ncs new-cell)))
      (str ncs (subs cs 1)))))

(defn generate [n cells]
  (when (pos? n)
    (cons cells
          (generate (dec n) (next-gen cells)))))

(defn -main [& args]
  (let [n (parse-long (or (first args) "10"))
        cells "_###_##_#_#_#_#__#__"]
    (doseq [line (generate n cells)]
      (println line))))
