;; https://rosettacode.org/wiki/Greatest_common_divisor#Clojure

(ns main.core)

(defn gcd
  "(gcd a b) computes the greatest common divisor of a and b."
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

(defn -main [x y]
  (println
   (str "gcd(" x " " y ") -> "
        (gcd (parse-long x) (parse-long y)))))
