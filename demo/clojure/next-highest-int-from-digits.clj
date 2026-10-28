;; https://rosettacode.org/wiki/Next_highest_int_from_digits#Clojure

(ns main.core)

(defn digits [n]
  (vec (map #(- (int %) (int \0)) (seq (str n)))))

(defn next-highest-int-from-digits [n]
  (let [ds (digits n)
        len (count ds)
        pivot-count (loop [idx (dec len)
                           cnt 0]
                      (if (and (pos? idx)
                               (>= (nth ds (dec idx)) (nth ds idx)))
                        (recur (dec idx) (inc cnt))
                        cnt))
        i (- (dec len) pivot-count)]
    (if (pos? i)
      (let [[left right] (split-at i ds)
            [a b-vec] (split-at (dec (count left)) left)
            b (first b-vec)
            c-sorted (vec (sort right))
            [c-part d-part] (split-with #(>= b %) c-sorted)
            [d e-part] (split-at 1 d-part)
            e (vec (sort (concat [b] c-part e-part)))
            result (concat a d e)]
        (parse-long (apply str result)))
      0)))

(defn -main [& args]
  (let [n (parse-long (or (first args) "12453"))]
    (println (next-highest-int-from-digits n))))
