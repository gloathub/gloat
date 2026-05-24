(ns boolean-test
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "parse-true"   (println (Boolean/parseBoolean "true"))
      "parse-TRUE"   (println (Boolean/parseBoolean "TRUE"))
      "parse-yes"    (println (Boolean/parseBoolean "yes"))
      "parse-empty"  (println (Boolean/parseBoolean ""))
      "valueof-str"  (println (Boolean/valueOf "True"))
      "valueof-bool" (println (Boolean/valueOf true))
      "tostring"     (println (Boolean/toString true))
      "tostring-f"   (println (Boolean/toString false))
      "compare-lt"   (println (Boolean/compare false true))
      "compare-eq"   (println (Boolean/compare true true))
      "and"          (println (Boolean/logicalAnd true false))
      "or"           (println (Boolean/logicalOr true false))
      "xor"          (println (Boolean/logicalXor true true))
      "TRUE"         (println Boolean/TRUE)
      "FALSE"        (println Boolean/FALSE)
      "ctor-str"     (println (Boolean. "true"))
      "ctor-bool"    (println (Boolean. false))
      "fq-parse"     (println (java.lang.Boolean/parseBoolean "true"))
      (println "usage: boolean <op>"))))
