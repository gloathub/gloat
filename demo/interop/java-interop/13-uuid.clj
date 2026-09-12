;; Calling java.util.UUID static members and instance methods.
;;
;; Same three-layer flow as the java.lang.* classes: gloat rewrites each
;; `UUID/*` symbol to glojure's javacompat bridge, which forwards to
;; gojava's typed Go port. The (UUID. msb lsb) constructor sugar is
;; rewritten to fromBits at AOT time.
;;
;; UUID instance methods (toString, version, variant, compareTo,
;; getMostSignificantBits, ...) are reached at runtime via reflection on
;; the *UUID receiver. The Go type also implements fmt.Stringer so
;; (str u) and println yield the canonical hex form.

(ns main.core)

(defn -main []
  ;; Random v4
  (let [u (UUID/randomUUID)]
    (println "random version:"  (.version u))
    (println "random variant:"  (.variant u))
    (println "random string:"   (.toString u)))

  ;; Parsing
  (let [u (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")]
    (println "round-trip:" (.toString u))
    (println "msb:"        (.getMostSignificantBits u))
    (println "lsb:"        (.getLeastSignificantBits u)))

  ;; Name-based (v3, MD5 of the input bytes; matches Java's
  ;; nameUUIDFromBytes which prepends no namespace)
  (let [u (UUID/nameUUIDFromBytes (.getBytes "hello"))]
    (println "named version:" (.version u))
    (println "named string:"  (.toString u)))

  ;; (UUID. msb lsb) constructor sugar
  (let [u (UUID. 42 99)]
    (println "ctor toString:" (.toString u))
    (println "ctor msb:"      (.getMostSignificantBits u))
    (println "ctor lsb:"      (.getLeastSignificantBits u)))

  ;; Equality and ordering
  (println "equals same:"  (.equals  (UUID. 1 2) (UUID. 1 2)))
  (println "equals diff:"  (.equals  (UUID. 1 2) (UUID. 1 3)))
  (println "compareTo lt:" (.compareTo (UUID. 1 2) (UUID. 1 3)))
  (println "compareTo gt:" (.compareTo (UUID. 2 0) (UUID. 1 9)))

  ;; HashCode is stable across parses of the same string
  (let [a (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")
        b (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")]
    (println "hashCode stable:" (= (.hashCode a) (.hashCode b))))

  ;; Fully qualified form also resolves
  (println "fq fromString:"
           (.toString (java.util.UUID/fromString
                        "ffffffff-ffff-4fff-8fff-ffffffffffff"))))
