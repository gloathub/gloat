; https://rosettacode.org/wiki/Weird_numbers#Clojure

(ns main.core)

(defn sqrt [x]
  (let [epsilon 1e-15
        abs-fn (fn [n] (if (< n 0) (- n) n))]
    (loop [guess 1.0]
      (let [next-guess (/ (+ guess (/ x guess)) 2)]
        (if (< (abs-fn (- next-guess guess)) epsilon)
          next-guess
          (recur next-guess))))))

(defn divisors [number]
  (let [divisors
        (mapcat
         (fn [n] (when (= 0 (rem number n)) [n (quot number n)]))
         (range 1 (sqrt number)))]
    (rest (sort > (distinct divisors)))))

(defn abundant [n divs] (> (apply + divs) n))

(defn semiperfect [n divs]
  (when (> (count divs) 0)
    (let [[div & divs] divs]
      (if (< n div)
        (semiperfect n divs)
        (or
         (= n div)
         (semiperfect (- n div) divs)
         (semiperfect n divs))))))

(defn sieve [limit]
  (let [weirds (into [] (repeat limit true))]
    (loop [i 2 weirds weirds]
      (if (< i limit)
        (recur
         (+ i 2)
         (or
          (when (get weirds i)
            (let [divs (divisors i)]
              (if (abundant i divs)
                (when (semiperfect i divs)
                  (loop [j i weirds weirds]
                    (if (< j limit)
                      (recur (+ j i) (assoc weirds j false))
                      weirds)))
                (assoc weirds i false))))
          weirds))
        weirds))))

(defn join-with-space [coll]
  (apply str (interpose " " coll)))

(defn -main [& args]
  (let [max (if (seq args) (read-string (first args)) 16500)
        weird (sieve max)
        numbers (filter #(nth weird %) (range 2 max 2))]
    (println
     (str
      "The first " (count numbers) " weird numbers:\n"
      (join-with-space numbers)
      "\n"))))
