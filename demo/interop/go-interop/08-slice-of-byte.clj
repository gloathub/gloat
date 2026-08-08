;; Building a `[]byte` for a Go API that wants one.
;;
;; `(go/slice-of go/byte)` returns a constructor for Go's `[]byte`;
;; applying it to a string produces the byte slice. The same
;; `go/slice-of` shape works for any element type when a Go API insists
;; on a slice rather than a Glojure seq.

(ns main.core)

(defn -main []
  (let [bs       ((go/slice-of go/byte) "foo----bar")
        [m err]  (regexp.Match "foo.*bar" bs)]
    (println "bytes count:" (count bs))
    (println "matched?    " m)
    (println "err:        " err)))
