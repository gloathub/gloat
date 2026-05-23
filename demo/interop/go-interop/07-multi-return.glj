;; Destructuring Go's `(value, error)` returns.
;;
;; Functions returning multiple values, like `strconv.Atoi`, come back
;; as a Glojure vector and bind with normal destructuring. A successful
;; call has `nil` in the error slot; a failed call holds a Go `error`
;; whose message is read with `(.Error err)`.

(ns main.core)

(defn -main []
  (let [[n   err]  (strconv.Atoi "42")
        [n2  err2] (strconv.Atoi "not-a-number")]
    (println "good input ->" n "  err:" err)
    (println "bad input  ->" n2 "  err:" err2)
    (when (some? err2)
      (println "  error message:" (.Error err2)))))
