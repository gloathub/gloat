;; https://rosettacode.org/wiki/Yellowstone_sequence#YAMLScript

(ns main.core
  (:require [clojure.string :as str]))

(defn gcd [a b]
  (loop [a a, b b]
    (if (zero? b)
      a
      (recur b (mod a b)))))

(defn yellowstone [n]
  (loop [a [1 2 3]
         b {1 true, 2 true, 3 true}
         i 4]
    (if (> n (count a))
      (if (and (not (get b i))
               (= 1 (gcd i (last a)))
               (> (gcd i (last (butlast a))) 1))
        (recur (conj a i) (assoc b i true) 5)
        (recur a b (inc i)))
      a)))

(defn -main [& args]
  (let [n (parse-long (or (first args) "30"))
        result (yellowstone n)]
    (println (str/join " " result))))
