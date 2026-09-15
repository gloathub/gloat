;; https://rosettacode.org/wiki/Leap_year#Clojure

(ns main.core)

(defn leap-year? [y]
  (and (zero? (mod y 4))
       (or (pos?  (mod y 100))
           (zero? (mod y 400)))))

(defn -main [& args]
  (let [year (parse-long (or (first args) "2024"))]
    (println
     (str year " is "
          (if (leap-year? year) "" "not ")
          "a leap year"))))
