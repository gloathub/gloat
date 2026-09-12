;; https://rosettacode.org/wiki/Luhn_test_of_credit_card_numbers#Clojure

(ns main.core)

(defn luhn? [cc]
  (let [factors (cycle [1 2])
        numbers (map #(- (int %) (int \0)) cc)
        sum (reduce + (map #(+ (quot % 10) (mod % 10))
                           (map * (reverse numbers) factors)))]
    (zero? (mod sum 10))))

(defn -main [s]
  (println
   s "-"
   (if (luhn? s) "valid" "invalid")))
