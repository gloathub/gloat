;; https://rosettacode.org/wiki/Array_length#Clojure

(ns main.core)

(defn -main [& args]
  (let [exprs [["apple" "orange"]
               (vec args)
               (range 10 20)
               (vec (range (int \A) (inc (int \Z))))
               "Hello, world!"]]
    (doseq [expr exprs]
      (println (str "Length: " (count expr))))))
