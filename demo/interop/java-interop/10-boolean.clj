;; Calling java.lang.Boolean static members.
;;
;; Same three-layer flow as the other java.lang.* classes: gloat rewrites
;; each `Boolean/*` symbol to glojure's javacompat bridge, which forwards
;; to gojava's typed Go port.
;;
;; `parseBoolean` and `valueOf` are the JVM-lenient form: they return
;; `true` iff the input matches "true" case-insensitively, and `false`
;; for everything else (including the empty string).

(ns main.core)

(defn -main []
  ;; Parsing
  (println "parseBoolean true:"  (Boolean/parseBoolean "true"))
  (println "parseBoolean TRUE:"  (Boolean/parseBoolean "TRUE"))
  (println "parseBoolean yes:"   (Boolean/parseBoolean "yes"))
  (println "parseBoolean empty:" (Boolean/parseBoolean ""))

  ;; valueOf accepts string or bool
  (println "valueOf \"True\":"   (Boolean/valueOf "True"))
  (println "valueOf true:"       (Boolean/valueOf true))

  ;; Constants
  (println "TRUE:"  Boolean/TRUE)
  (println "FALSE:" Boolean/FALSE)

  ;; Conversion and comparison
  (println "toString true:"      (Boolean/toString true))
  (println "compare false true:" (Boolean/compare false true))

  ;; Logical combinators (point-free function form)
  (println "logicalAnd t f:" (Boolean/logicalAnd true false))
  (println "logicalOr  t f:" (Boolean/logicalOr true false))
  (println "logicalXor t t:" (Boolean/logicalXor true true))

  ;; Constructor sugar rewrites to valueOf
  (println "(Boolean. \"true\"):" (Boolean. "true"))
  (println "(Boolean. false):"    (Boolean. false))

  ;; Fully qualified form also resolves
  (println "fq parseBoolean:" (java.lang.Boolean/parseBoolean "true")))
