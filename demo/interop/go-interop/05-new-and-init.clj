;; Constructing a struct with `new`, then initialising fields.
;;
;; `(new T)` allocates a zero-valued `*T` (Go's `new(T)`), and subsequent
;; `set!` calls populate fields. `(int ...)` is needed when the target
;; field is a sized integer type and the source is a Glojure number,
;; which is otherwise an arbitrary-precision value.

(ns main.core)

(defn -main []
  (let [srv (new net:http.Server)]
    (set! (. srv Addr)           "localhost:8080")
    (set! (. srv MaxHeaderBytes) (int (* 1024 16)))
    (println "Addr:          " (. srv Addr))
    (println "MaxHeaderBytes:" (. srv MaxHeaderBytes))))
