; https://rosettacode.org/wiki/FizzBuzz#Clojure

(ns main.core)

(defn -main [& args]
  (let [n (if (seq args)
            (read-string (first args))
            100)]
    (doseq [x (map (fn [x] (cond (zero? (mod x 15)) "FizzBuzz"
                                 (zero? (mod x 5)) "Buzz"
                                 (zero? (mod x 3)) "Fizz"
                                 :else x))
                   (range 1 (inc n)))]
      (println x))))
