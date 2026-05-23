(ns java-lang-fq
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "math-abs"   (println (java.lang.Math/abs -42))
      "math-sqrt"  (println (java.lang.Math/sqrt 16))
      "math-pi"    (println java.lang.Math/PI)
      "math-pow"   (println (java.lang.Math/pow 2 10))
      "int-parse"  (println (java.lang.Integer/parseInt "42"))
      "int-max"    (println java.lang.Integer/MAX_VALUE)
      "int-bin"    (println (java.lang.Integer/toBinaryString 42))
      "int-ctor"   (println (java.lang.Integer. 100))
      "long-parse" (println (java.lang.Long/parseLong "9999999999"))
      "long-max"   (println java.lang.Long/MAX_VALUE)
      "long-ctor"  (println (java.lang.Long. 123456789012345))
      "sys-line"   (let [s (java.lang.System/lineSeparator)]
                     (println (count s)))
      "sys-env"    (println (some? (java.lang.System/getenv "PATH")))
      (println "usage: java-lang-fq <op>"))))
