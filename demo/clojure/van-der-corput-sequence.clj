;; https://rosettacode.org/wiki/Van_der_Corput_sequence#Clojure

(ns main.core)

(defn van-der-corput
  ([n] (van-der-corput n 2))
  ([n base]
   (let [s (/ 1 base)]
     (loop [sum 0.0
            n n
            scale s]
       (if (zero? n)
         sum
         (recur (+ sum (* (rem n base) scale))
                (quot n base)
                (* scale s)))))))

(defn -main [& args]
  (let [base (parse-long (or (first args) "2"))]
    (doseq [n (range 10)]
      (println (format "%.6f" (van-der-corput n base))))))
