;; https://rosettacode.org/wiki/Sieve_of_Eratosthenes#Clojure

(ns main.core
  (:require [clojure.string :as str]))

(defn sieve [n]
  (loop [primes (vec (range n))
         i 2]
    (if (>= (* i i) n)
      (filter #(and % (> % 1)) primes)
      (if (get primes i)
        (recur
         (loop [p primes
                j (* i i)]
           (if (< j n)
             (recur (assoc p j nil) (+ j i))
             p))
         (inc i))
        (recur primes (inc i))))))

(defn -main [& args]
  (let [n (parse-long (or (first args) "100"))
        primes (sieve n)
        count (count primes)]
    (println (str "The " count " prime numbers less than " n " are:"))
    (println (str/join " " primes))))
