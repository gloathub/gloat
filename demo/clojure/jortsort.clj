;; https://rosettacode.org/wiki/JortSort#Clojure

(ns main.core)

(defn jort-sort? [x]
  (= x (sort x)))

(defn -main [& args]
  (let [array (vec (map parse-long args))
        sorted? (jort-sort? array)]
    (println (str array (if sorted? " is sorted" " is not sorted")))))
