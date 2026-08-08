;; Reading Java static fields.
;;
;; `Math.PI` and `Math.E` are static constants on java.lang.Math. In
;; Glojure they are referenced as `Math/PI` and `Math/E`, in non-call
;; position. The same Class/NAME form covers any future
;; `Integer/MAX_VALUE`, `Float/POSITIVE_INFINITY`, etc.

(ns main.core)

(defn -main []
  (println Math/PI)                    ; 3.141592653589793
  (println Math/E)                     ; 2.718281828459045
  (println (Math/toRadians 180)))      ; PI (180 degrees in radians)
