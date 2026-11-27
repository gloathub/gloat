;; https://rosettacode.org/wiki/Sparkline_in_unicode#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn sparkline [nums]
  (let [sparks   "▁▂▃▄▅▆▇█"
        high     (apply max nums)
        low      (apply min nums)
        spread   (- high low)
        quantize (fn [x] (long (* 7 (/ (- x low) spread))))]
    (apply str (map #(nth sparks (quantize %)) nums))))

(defn -main [& args]
  (let [nums (map parse-long args)]
    (println (sparkline nums))))
