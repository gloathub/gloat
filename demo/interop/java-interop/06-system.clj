;; Calling java.lang.System static members.
;;
;; Same three-layer flow as Math/*: gloat rewrites each `System/*`
;; symbol to glojure's javacompat bridge, which forwards to gojava's
;; typed Go port of `java.lang.System`.
;;
;; The streams `System/out`, `System/err`, and `System/in` are thin
;; wrappers over the matching `*os.File`. Glojure's instance-method
;; resolution capitalizes the first letter, so `(.println System/out x)`
;; lands on the Go method `Println(x)`.

(ns main.core)

(defn -main []
  (println "millis>0:" (> (System/currentTimeMillis) 0))
  (println "nanos>0:"  (> (System/nanoTime) 0))

  (println "user.home set:" (some? (System/getProperty "user.home")))
  (println "os.name:"       (System/getProperty "os.name"))
  (println "default:"       (System/getProperty "no.such.key" "fallback"))

  (println "PATH set:" (some? (System/getenv "PATH")))
  (println "bogus:"    (System/getenv "GLOAT_BOGUS_KEY_XYZ"))

  (System/setProperty "demo.key" "demo-value")
  (println "round-trip:" (System/getProperty "demo.key"))
  (println "cleared:"    (System/clearProperty "demo.key"))
  (println "after:"      (System/getProperty "demo.key"))

  (.println System/out "stdout via (.println System/out ...)")
  (.println System/err "stderr via (.println System/err ...)"))
