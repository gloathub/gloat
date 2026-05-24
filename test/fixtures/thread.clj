(ns thread-test
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "sleep-zero"   (do (Thread/sleep 0) (println "ok"))
      "sleep-short"  (do (Thread/sleep 5) (println "ok"))
      "sleep-nanos"  (do (Thread/sleep 1 500000) (println "ok"))
      "fq-sleep"     (do (java.lang.Thread/sleep 1) (println "ok"))
      "elapsed-ge"   (let [t0 (System/currentTimeMillis)
                           _  (Thread/sleep 20)
                           dt (- (System/currentTimeMillis) t0)]
                       (println (>= dt 20))))))
