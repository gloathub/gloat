;; https://rosettacode.org/wiki/Floyd%27s_triangle#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn triangle-list [n]
  (let [l (map inc (range))]
    (loop [l l x 1 nl []]
      (if (= n (count nl))
        nl
        (recur (drop x l) (inc x) (conj nl (take x l)))))))

(defn triangle-print [n]
  (let [t (triangle-list n)
        m (count (str (last (last t))))
        f (map #(map str %) t)
        l (map #(map (fn [x] (if (> m (count x))
                               (str (apply str (take (- m (count x))
                                                     (repeat " "))) x)
                               x)) %) f)
        e (map #(map (fn [x] (str " " x)) %) l)]
    (doseq [row e]
      (println (apply str row)))))

(defn -main [& args]
  (let [n (parse-long (or (first args) "14"))]
    (triangle-print n)))
