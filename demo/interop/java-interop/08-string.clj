;; Calling java.lang.String static and instance members.
;;
;; Three-layer flow as Math/*, Integer/*, System/*: static `String/*`
;; symbols rewrite to glojure's javacompat bridge; instance `.method`
;; forms dispatch at runtime through the lang string-method registry.
;;
;; Method semantics match the JVM: `.length` returns UTF-16 code units,
;; `.trim` strips ASCII whitespace only, `.hashCode` runs Java's
;; s[0]*31^(n-1) + ... formula with int32 wraparound, `String/format`
;; accepts `%n`, `%b`, and positional %N$x indices.

(ns main.core)

(defn -main []
  ;; Length and predicates
  (println "length hello:" (.length "hello"))
  (println "length naïve:" (.length "naïve"))
  (println "isEmpty:"      (.isEmpty ""))
  (println "isBlank:"      (.isBlank "  \t "))

  ;; Case folding
  (println "upper:"        (.toUpperCase "hello"))
  (println "lower:"        (.toLowerCase "HELLO"))

  ;; Trimming and stripping
  (println "trim:"          (str "[" (.trim "  hi  ") "]"))
  (println "strip:"         (str "[" (.strip "   hi   ") "]"))
  (println "stripLeading:"  (str "[" (.stripLeading "  hi  ") "]"))
  (println "stripTrailing:" (str "[" (.stripTrailing "  hi  ") "]"))

  ;; Substrings and indexOf
  (println "substring:"   (.substring "hello world" 6))
  (println "substring 2:" (.substring "hello world" 0 5))
  (println "indexOf:"     (.indexOf "hello world" "world"))
  (println "lastIndexOf:" (.lastIndexOf "ababab" "ab"))
  (println "startsWith:"  (.startsWith "hello world" "hello"))
  (println "endsWith:"    (.endsWith "hello world" "world"))
  (println "contains:"    (.contains "hello world" "lo wo"))

  ;; Comparison
  (println "equals:"           (.equals "abc" "abc"))
  (println "equalsIgnoreCase:" (.equalsIgnoreCase "ABC" "abc"))
  (println "compareTo:"        (.compareTo "abc" "abd"))

  ;; Construction-style ops
  (println "concat:"  (.concat "foo" "bar"))
  (println "repeat:"  (.repeat "ab" 3))
  (println "replace:" (.replace "foobar" "oo" "OO"))
  (println "split:"   (vec (.split "a,b,c" ",")))

  ;; Hash matches JVM exactly
  (println "hashCode hello:" (.hashCode "hello"))

  ;; Statics: format, join, valueOf, ctor sugar
  (println "format:"  (String/format "%s=%d" "x" 42))
  (println "join:"    (String/join "-" ["a" "b" "c"]))
  (println "valueOf nil:"  (String/valueOf nil))
  (println "valueOf true:" (String/valueOf true))
  (println "(String. \"x\"):" (String. "x"))

  ;; Fully qualified java.lang.String/* also resolves
  (println "fq format:" (java.lang.String/format "n=%d" 7)))
