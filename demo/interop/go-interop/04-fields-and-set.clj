;; Reading and writing struct fields.
;;
;; `(. obj Field)` reads a field; `(set! (. obj Field) value)` writes
;; one. The colonified path `net:http` resolves to Go's `net/http`
;; package; the type is referenced as `net:http.Server`. Field reads
;; return Go's zero value when unset.

(ns main.core)

(defn -main []
  (let [srv (new net:http.Server)]
    (println "Addr before:" (pr-str (. srv Addr)))
    (set! (. srv Addr) ":8080")
    (println "Addr after: " (pr-str (. srv Addr)))))
