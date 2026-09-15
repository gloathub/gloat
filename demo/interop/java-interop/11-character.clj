;; Calling java.lang.Character static members.
;;
;; Same three-layer flow as the other java.lang.* classes: gloat rewrites
;; each `Character/*` symbol to glojure's javacompat bridge, which forwards
;; to gojava's typed Go port.
;;
;; Glojure parses character literals (`\a`, `\5`, `\space`) as the wrapper
;; type `lang.Char` (a rune). The bridge unwraps it so Character/* sees a
;; plain rune; you can also pass an integer code point directly. Predicates
;; consult Go's `unicode` package, which keeps them in sync with the same
;; categories the JVM uses.

(ns main.core)

(defn -main []
  ;; Predicates
  (println "isDigit \\5:"        (Character/isDigit \5))
  (println "isLetter \\x:"       (Character/isLetter \x))
  (println "isLetterOrDigit:"    (Character/isLetterOrDigit \3))
  (println "isUpperCase \\X:"    (Character/isUpperCase \X))
  (println "isLowerCase \\X:"    (Character/isLowerCase \X))
  (println "isWhitespace \\tab:" (Character/isWhitespace \tab))
  (println "isAlphabetic \\z:"   (Character/isAlphabetic \z))

  ;; Case folding
  (println "toUpperCase \\a:" (Character/toUpperCase \a))
  (println "toLowerCase \\Z:" (Character/toLowerCase \Z))

  ;; Conversion to string
  (println "toString \\k:" (Character/toString \k))

  ;; Radix-aware digit conversion
  (println "digit \\f 16:"   (Character/digit \f 16))
  (println "digit \\z 10:"   (Character/digit \z 10))   ; -1: out of radix
  (println "forDigit 10 16:" (Character/forDigit 10 16))
  (println "forDigit 9 10:"  (Character/forDigit 9 10))
  (println "numericValue 7:" (Character/getNumericValue \7))

  ;; Comparison
  (println "compare \\a \\b:" (Character/compare \a \b))

  ;; Constants
  (println "MIN_RADIX:" Character/MIN_RADIX)
  (println "MAX_RADIX:" Character/MAX_RADIX)

  ;; Constructor sugar rewrites to valueOf
  (println "(Character. \\W):" (Character. \W))

  ;; Fully qualified form also resolves
  (println "fq isDigit:" (java.lang.Character/isDigit \1)))
