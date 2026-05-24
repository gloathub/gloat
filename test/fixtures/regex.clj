(ns regex-test
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "matches-true"   (println (Pattern/matches "\\d+" "12345"))
      "matches-false"  (println (Pattern/matches "\\d+" "12a45"))
      "quote"          (println (Pattern/quote "a.b*c"))
      "ci-const"       (println Pattern/CASE_INSENSITIVE)
      "multi-const"    (println Pattern/MULTILINE)
      "dotall-const"   (println Pattern/DOTALL)
      "literal-const"  (println Pattern/LITERAL)
      "find"           (let [p (Pattern/compile "(\\d+)-(\\w+)")
                             m (.matcher p "42-foo bar")]
                         (println (.find m)))
      "group0"         (let [p (Pattern/compile "(\\d+)-(\\w+)")
                             m (.matcher p "42-foo bar")]
                         (.find m)
                         (println (.group m)))
      "group1"         (let [p (Pattern/compile "(\\d+)-(\\w+)")
                             m (.matcher p "42-foo bar")]
                         (.find m)
                         (println (.group m 1)))
      "group2"         (let [p (Pattern/compile "(\\d+)-(\\w+)")
                             m (.matcher p "42-foo bar")]
                         (.find m)
                         (println (.group m 2)))
      "start"          (let [p (Pattern/compile "foo")
                             m (.matcher p "bar foo baz")]
                         (.find m)
                         (println (.start m)))
      "end"            (let [p (Pattern/compile "foo")
                             m (.matcher p "bar foo baz")]
                         (.find m)
                         (println (.end m)))
      "matches"        (let [p (Pattern/compile "\\d+")
                             m (.matcher p "123")]
                         (println (.matches m)))
      "lookingat"      (let [p (Pattern/compile "\\d+")
                             m (.matcher p "123abc")]
                         (println (.lookingAt m)))
      "groupcount"     (let [p (Pattern/compile "(\\d+)-(\\w+)")
                             m (.matcher p "1-x")]
                         (println (.groupCount m)))
      "split"          (let [p (Pattern/compile "\\s+")]
                         (println (vec (.split p "foo  bar\tbaz"))))
      "split-limit"    (let [p (Pattern/compile ",")]
                         (println (vec (.splitLimit p "a,b,c,,," -1))))
      "ci-flag"        (let [p (Pattern/compile "HELLO" Pattern/CASE_INSENSITIVE)
                             m (.matcher p "hello world")]
                         (println (.find m)))
      "replaceall"     (let [p (Pattern/compile "\\d+")
                             m (.matcher p "a1 b22 c333")]
                         (println (.replaceAll m "N")))
      "replacefirst"   (let [p (Pattern/compile "\\d+")
                             m (.matcher p "a1 b22 c333")]
                         (println (.replaceFirst m "N")))
      "fq-compile"     (let [p (java.util.regex.Pattern/compile "x+")
                             m (.matcher p "xxxxy")]
                         (.find m)
                         (println (.group m)))
      "ctor"           (let [p (Pattern. "[abc]+")
                             m (.matcher p "abcabc xxx")]
                         (.find m)
                         (println (.group m)))
      "pattern-str"    (let [p (Pattern/compile "\\d+")]
                         (println (.pattern p)))
      (println "usage: regex <op>"))))
