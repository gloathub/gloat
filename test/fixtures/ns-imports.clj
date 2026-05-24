(ns ns-imports
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)
        imports (ns-imports *ns*)
        has? (fn [name] (contains? imports (symbol name)))]
    (case op
      "count-pos"  (println (> (count imports) 0))
      "has-math"   (println (has? "Math"))
      "has-system" (println (has? "System"))
      "has-thread" (println (has? "Thread"))
      "has-string" (println (has? "String"))
      "has-pattern" (println (has? "Pattern"))
      "has-uuid"   (println (has? "UUID"))
      "has-instant" (println (has? "Instant"))
      "twelve"     (println (>= (count imports) 12))
      "dump"       (doseq [k (sort (map str (keys imports)))] (println k))
      "show-math"  (println (str (get imports (symbol "Math"))))
      "show-uuid"  (println (str (get imports (symbol "UUID"))))
      "pr-math"    (pr (get imports (symbol "Math")))
      "sym-math"   (println Math)
      "sym-fq-math" (println java.lang.Math)
      "sym-integer" (println Integer)
      "sym-fq-integer" (println java.lang.Integer)
      "count"      (println (count imports))
      (println (str "unknown op: " op))))
  (System/exit 0))
