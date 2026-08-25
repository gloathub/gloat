;; JVM-faithful semantics where Go's stdlib differs.
;;
;; Several `java.lang.Math` operations have specific rounding or overflow
;; behaviour that is NOT the same as the equivalent Go `math` function.
;; The bridge guarantees the JVM result. Compare the comments below to
;; what `math.Round`, integer division, or `%` would give you in Go.

(ns main.core)

(defn -main []
  ;; Math/round rounds half-values toward positive infinity.
  ;; Go's math.Round rounds half-values away from zero.
  (println (Math/round 2.5))           ; 3   (Go math.Round also gives 3)
  (println (Math/round -2.5))          ; -2  (Go math.Round gives -3)

  ;; Math/floorDiv and Math/floorMod use floor semantics for negatives.
  ;; Go's `/` and `%` operators truncate toward zero.
  (println (Math/floorDiv -7 2))       ; -4  (Go -7/2 = -3)
  (println (Math/floorMod -7 2)))      ; 1   (Go -7%2 = -1)
