;; https://rosettacode.org/wiki/Validate_International_Securities_Identification_Number#Clojure

(ns main.core)

(defn luhn? [cc]
  (let [sum (->> cc
                 (map #(- (int %) (int \0)))
                 reverse
                 (map * (cycle [1 2]))
                 (map #(+ (quot % 10) (mod % 10)))
                 (reduce +))]
    (zero? (mod sum 10))))

(defn char->base36 [c]
  (let [code (int c)]
    (cond
      (<= (int \0) code (int \9)) (- code (int \0))
      (<= (int \A) code (int \Z)) (+ 10 (- code (int \A)))
      :else 0)))

(defn is-valid-isin? [isin]
  (and (re-matches #"^[A-Z]{2}[A-Z0-9]{9}[0-9]$" isin)
       (->> isin
            (map char->base36)
            (apply str)
            luhn?)))

(defn -main [s]
  (println (str s " - " (if (is-valid-isin? s) "valid" "invalid"))))
