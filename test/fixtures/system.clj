(ns system
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "millis-pos"  (println (> (System/currentTimeMillis) 0))
      "nanos-pos"   (println (> (System/nanoTime) 0))
      "env-set"     (println (some? (System/getenv "PATH")))
      "env-unset"   (println (System/getenv "GLOAT_BOGUS_KEY_XYZ"))
      "env-value"   (println (System/getenv "GLOAT_TEST_KEY"))
      "env-all"     (println (contains? (System/getenv) "PATH"))
      "prop-home"   (println (some? (System/getProperty "user.home")))
      "prop-unset"  (println (System/getProperty "no.such.key"))
      "prop-or"     (println (System/getProperty "no.such.key" "fallback"))
      "prop-set"    (let [_ (System/setProperty "k" "v1")
                          v (System/getProperty "k")]
                      (println v))
      "prop-clear"  (let [_   (System/setProperty "k" "v1")
                          old (System/clearProperty "k")
                          now (System/getProperty "k")]
                      (println old now))
      "line-sep"    (println (count (System/lineSeparator)))
      "stdout"      (.println System/out "via stdout")
      "stderr"      (.println System/err "via stderr")
      "gc"          (do (System/gc) (println "ok"))
      (println "usage: system <op>"))))
