(ns math
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "pi"        (println Math/PI)
      "e"         (println Math/E)
      "sqrt"      (println (Math/sqrt 144))
      "pow"       (println (Math/pow 2 10))
      "abs-long"  (println (Math/abs -42))
      "abs-dbl"   (println (Math/abs -3.5))
      "floor"     (println (Math/floor 2.7))
      "ceil"      (println (Math/ceil 2.3))
      "round-up"  (println (Math/round 2.5))
      "round-dn"  (println (Math/round -2.5))
      "floordiv"  (println (Math/floorDiv -7 2))
      "floormod"  (println (Math/floorMod -7 2))
      "hypot"     (println (Math/hypot 3 4))
      "atan2"     (println (Math/atan2 1 1))
      "tor"       (println (Math/toRadians 180.0))
      (println "usage: math <op>"))))
