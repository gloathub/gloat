;; Java-style and Go-style interop side by side.
;;
;; Both forms work in the same file. They are additive, never
;; either/or.
;;
;;   `(Math/sqrt x)`    - JVM-style; gets JVM-faithful semantics from the
;;                        javacompat bridge.
;;   `(math.Sqrt x)`    - Go-style; calls Go's stdlib `math.Sqrt`
;;                        directly. Case matters: Go names are
;;                        capitalised.
;;
;; For `sqrt` the two paths happen to produce identical output. The
;; difference shows up in the cases from 04-jvm-semantics: Go's stdlib
;; will not give you JVM-faithful `floorDiv` or `round(-2.5)`.

(ns main.core)

(defn -main []
  (println (Math/sqrt 144))            ; 12.0   (JVM-style)
  (println (math.Sqrt 144))            ; 12.0   (Go stdlib)
  (println (Math/abs -42))             ; 42     (JVM-style; polymorphic)
  (println (math.Abs -42.0)))          ; 42.0   (Go stdlib; float64 only)
