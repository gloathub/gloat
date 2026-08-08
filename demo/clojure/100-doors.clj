; https://rosettacode.org/wiki/100_doors#Clojure

(ns main.core)

(defn open-doors []
  (->> (iterate inc 1) (map #(* % %)) (take-while #(<= % 100))))

(defn print-open-doors []
  (println
   "Open doors after 100 passes:"
   (apply str (interpose ", " (open-doors)))))

(defn -main []
  (print-open-doors))
