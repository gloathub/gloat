;; https://rosettacode.org/wiki/Integer_comparison#Clojure

(ns main.core)

(defn -main [& args]
  (let [a (parse-long (or (first args) "5"))
        b (parse-long (or (second args) "10"))]
    (cond
      (< a b) (println (str a " is less than " b))
      (> a b) (println (str a " is greater than " b))
      :else   (println (str a " is equal to " b)))))
