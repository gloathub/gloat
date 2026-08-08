;; Calling java.lang.Integer and java.lang.Long static members.
;;
;; Same three-layer flow as Math/* and System/*: gloat rewrites each
;; `Integer/*` and `Long/*` symbol to glojure's javacompat bridge, which
;; forwards to gojava's typed Go ports.
;;
;; `Integer/MAX_VALUE` is an int32 with the JVM-faithful value 2^31 - 1,
;; not Go's platform-dependent `math.MaxInt`. `Long/MAX_VALUE` is the
;; full int64 max.

(ns main.core)

(defn -main []
  ;; Parsing
  (println "parseInt:"        (Integer/parseInt "42"))
  (println "parseInt binary:" (Integer/parseInt "1010" 2))
  (println "parseInt hex:"    (Integer/parseInt "ff" 16))
  (println "parseLong:"       (Long/parseLong "9999999999"))

  ;; Constants
  (println "Integer/MAX_VALUE:" Integer/MAX_VALUE)
  (println "Integer/MIN_VALUE:" Integer/MIN_VALUE)
  (println "Long/MAX_VALUE:"    Long/MAX_VALUE)
  (println "Long/MIN_VALUE:"    Long/MIN_VALUE)

  ;; Radix formatting
  (println "toBinaryString 42:" (Integer/toBinaryString 42))
  (println "toHexString 255:"   (Integer/toHexString 255))
  (println "toOctalString 8:"   (Integer/toOctalString 8))
  (println "Long toHex:"        (Long/toHexString 4096))

  ;; valueOf accepts int or string
  (println "valueOf int:" (Integer/valueOf 7))
  (println "valueOf str:" (Integer/valueOf "7"))

  ;; Constructor sugar rewrites to valueOf
  (println "(Integer. 5):"   (Integer. 5))
  (println "(Integer. \"5\"):" (Integer. "5"))
  (println "(Long. 12345):"  (Long. 12345))

  ;; Bit operations
  (println "bitCount 0xFF:"          (Integer/bitCount 0xFF))
  (println "leadingZeros 1:"         (Integer/numberOfLeadingZeros 1))
  (println "trailingZeros 8:"        (Integer/numberOfTrailingZeros 8))
  (println "Long/bitCount 0xFFFFFF:" (Long/bitCount 0xFFFFFF))

  ;; Comparisons
  (println "Integer/max 3 7:" (Integer/max 3 7))
  (println "Long/min 3 7:"    (Long/min 3 7))
  (println "signum -5:"       (Integer/signum -5)))
