;; https://rosettacode.org/wiki/Function_definition#Clojure

(ns main.core)

(defn multiply
  ([] 1)
  ([x] x)
  ([x y] (* x y))
  ([x y & more]
   (reduce multiply (multiply x y) more)))

(defn -main [& args]
  (let [nums (if (seq args)
               (map parse-long args)
               [2 3 4])
        result (apply multiply nums)]
    (println (str "multiply(" (clojure.string/join ", " nums) ") -> " result))))
