;; https://rosettacode.org/wiki/Factorial#Clojure

(ns main.core)

(defn factorial [x]
  (apply *' (range 2 (inc x))))

(defn -main [& args]
  (let [n (parse-long (or (first args) "10"))]
    (println (str n "! = " (factorial n)))))
