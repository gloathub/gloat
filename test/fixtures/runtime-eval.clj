(ns runtime-eval)

(defn -main [& _]
  (eval (read-string "(ns fib.core)"))
  (println (ns-name *ns*)))
