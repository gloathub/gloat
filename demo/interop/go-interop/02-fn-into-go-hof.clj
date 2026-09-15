;; Passing a Glojure `fn` to a Go higher-order function.
;;
;; `strings.FieldsFunc` takes a Go `func(rune) bool` predicate. A plain
;; Glojure `(fn [c] ...)` crosses the boundary as the matching Go
;; function value. The rune arrives as an integer code point; `(char c)`
;; converts it back for set membership.

(ns main.core)

(defn -main []
  (let [parts (strings.FieldsFunc
                "alpha,beta;gamma|delta"
                (fn [c] (contains? #{\, \; \|} (char c))))]
    (doseq [p parts]
      (println " -" p))))
