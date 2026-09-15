;; Calling java.util.regex.Pattern and the Matcher it produces.
;;
;; Same three-layer flow as the java.lang.* classes: gloat rewrites each
;; `Pattern/*` symbol to glojure's javacompat bridge, which forwards to
;; gojava's typed Go port. Matcher instance methods are reached at runtime
;; via reflection on the *Matcher receiver; the gojava package uses
;; capitalized variadic Go signatures so JVM overloads collapse to one
;; method per name.
;;
;; The engine is Go's regexp (RE2), so a few JVM features are absent:
;; possessive quantifiers (*+, ++, ?+), backreferences (\1, \k<name>),
;; and lookaround ((?=...), (?!...)). Java-style named groups
;; (?<name>...) are translated to RE2's (?P<name>...) form before
;; compilation, so the Java syntax works.

(ns main.core)

(defn -main []
  ;; One-shot whole-input match
  (println "matches digits:"   (Pattern/matches "\\d+" "12345"))
  (println "matches partial:"  (Pattern/matches "\\d+" "12a45"))

  ;; Quoting metacharacters
  (println "quote a.b*c:" (Pattern/quote "a.b*c"))

  ;; Flag constants
  (println "CASE_INSENSITIVE:" Pattern/CASE_INSENSITIVE)
  (println "MULTILINE:"        Pattern/MULTILINE)
  (println "DOTALL:"           Pattern/DOTALL)

  ;; Compile + drive a Matcher
  (let [p (Pattern/compile "(\\d+)-(\\w+)")
        m (.matcher p "42-foo bar 7-baz")]
    (println "find:"        (.find m))
    (println "group 0:"     (.group m))
    (println "group 1:"     (.group m 1))
    (println "group 2:"     (.group m 2))
    (println "start 1:"     (.start m 1))
    (println "end 2:"       (.end m 2))
    (println "groupCount:"  (.groupCount m))
    (println "find again:"  (.find m))
    (println "next group:"  (.group m)))

  ;; Split with no limit (trailing empties dropped, like Pattern.split)
  (let [p (Pattern/compile "\\s+")]
    (println "split:" (vec (.split p "foo  bar\tbaz"))))

  ;; Flag-aware compile: case-insensitive match
  (let [p (Pattern/compile "HELLO" Pattern/CASE_INSENSITIVE)
        m (.matcher p "well hello there")]
    (println "ci find:" (.find m)))

  ;; replaceAll / replaceFirst
  (let [p (Pattern/compile "\\d+")
        m (.matcher p "a1 b22 c333")]
    (println "replaceAll:" (.replaceAll m "N")))

  ;; Constructor sugar rewrites to compile
  (println "(Pattern. \"x+\"):" (.pattern (Pattern. "x+")))

  ;; Fully qualified form also resolves
  (println "fq matches:" (java.util.regex.Pattern/matches "[a-z]+" "abc")))
