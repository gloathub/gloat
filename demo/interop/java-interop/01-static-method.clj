;; Calling Java static methods.
;;
;; The JVM form `Math.sqrt(144)` becomes `(Math/sqrt 144)` in Glojure --
;; same syntax Clojure-on-the-JVM uses. Gloat rewrites each `Math/*`
;; symbol to a glojure-internal bridge that forwards to gojava's Go port
;; of `java.lang.Math`. No import or aliasing is required.

(ns main.core)

(defn -main []
  (println (Math/sqrt 144))            ; 12.0
  (println (Math/pow 2 10))            ; 1024.0
  (println (Math/floor 2.7))           ; 2.0
  (println (Math/ceil 2.3)))           ; 3.0
