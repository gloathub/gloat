;; Wrapping a Glojure fn as a typed Go function value.
;;
;; A bare Glojure fn satisfies a Go `func(...)` signature but does not
;; automatically satisfy an *interface* like `http.Handler`. Go's
;; `http.HandlerFunc` is an adapter type that turns a function into a
;; handler; calling `(net:http.HandlerFunc handler)` wraps the Glojure
;; fn the same way `http.HandlerFunc(handler)` does in Go.

(ns main.core)

(defn handler [w r]
  (. w (WriteHeader 200)))

(defn -main []
  (let [srv (new net:http.Server)]
    (set! (. srv Addr)    "localhost:8080")
    (set! (. srv Handler) (net:http.HandlerFunc handler))
    (let [field (.FieldByName (.Elem (reflect.ValueOf srv)) "Handler")]
      (println "Addr:    " (. srv Addr))
      (println "Handler: " (if (.IsNil field) "nil" "<set>")))))
