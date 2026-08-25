;; Calling java.lang.Double static members.
;;
;; Same three-layer flow as the integer classes: gloat rewrites each
;; `Double/*` symbol to glojure's javacompat bridge, which forwards to
;; gojava's typed Go port. Values are float64 to match Java's double.
;;
;; `Double/toString` emits JVM-style output: a decimal in the mantissa
;; ("1.0", not "1"), uppercase `E` exponent, no `+` for positive exponents,
;; and "NaN"/"Infinity"/"-Infinity" for the special values.

(ns main.core)

(defn -main []
  ;; Parsing
  (println "parseDouble:"      (Double/parseDouble "3.14"))
  (println "parseDouble inf:"  (Double/toString (Double/parseDouble "Infinity")))
  (println "valueOf string:"   (Double/valueOf "2.5"))
  (println "valueOf number:"   (Double/valueOf 4.0))

  ;; Constants
  (println "MAX_VALUE:"        (Double/toString Double/MAX_VALUE))
  (println "MIN_VALUE:"        (Double/toString Double/MIN_VALUE))
  (println "POSITIVE_INFINITY:" (Double/toString Double/POSITIVE_INFINITY))
  (println "NaN:"              (Double/toString Double/NaN))

  ;; Predicates
  (println "isNaN NaN:"        (Double/isNaN Double/NaN))
  (println "isInfinite inf:"   (Double/isInfinite Double/POSITIVE_INFINITY))
  (println "isFinite 1.0:"     (Double/isFinite 1.0))

  ;; Comparison
  (println "compare 1 2:"      (Double/compare 1.0 2.0))
  (println "max 3 7:"          (Double/max 3.0 7.0))
  (println "min 3 7:"          (Double/min 3.0 7.0))
  (println "sum:"              (Double/sum 1.5 2.5))

  ;; Bit conversion
  (println "doubleToLongBits 1.0:" (Double/doubleToLongBits 1.0))
  (println "longBitsToDouble:"     (Double/longBitsToDouble (Double/doubleToLongBits 3.14)))

  ;; Constructor sugar rewrites to valueOf
  (println "(Double. \"2.5\"):" (Double. "2.5"))
  (println "(Double. 4.0):"     (Double. 4.0))

  ;; Fully qualified form also resolves
  (println "fq parseDouble:"   (java.lang.Double/parseDouble "0.5")))
