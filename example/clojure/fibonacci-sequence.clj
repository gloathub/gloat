;; https://rosettacode.org/wiki/Fibonacci_sequence#Clojure

(ns main.core)

(defn- fib-iter
  [a b i n]
  (println a)
  (when (< i n)
    (recur b (+ a b) (inc i) n)))

(defn fibonacci [n]
  (when (> n 0)
    (fib-iter 0 1 1 n)))

(defn -main [& args]
  (let [n (parse-long (or (first args) "10"))]
    (fibonacci n)))
