;; Polymorphic dispatch on argument type.
;;
;; The JVM overloads `Math.abs` on int, long, float, and double, and the
;; return type matches the argument type. Go has no overloading -- gojava
;; exposes typed names (`AbsInt`, `AbsLong`, etc.) and the bridge picks
;; the right one at runtime based on the actual value type.
;;
;; The integer call returns an integer; the double call returns a double.

(ns main.core)

(defn -main []
  (println (Math/abs -42))             ; 42      (long stays long)
  (println (Math/abs -3.5))            ; 3.5     (double stays double)
  (println (Math/max 7 3))             ; 7       (long, long -> long)
  (println (Math/max 7.0 3)))          ; 7.0     (any double -> double)
