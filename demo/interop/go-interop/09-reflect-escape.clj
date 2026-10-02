;; Dropping to `reflect` when higher-level forms don't cover it.
;;
;; The direct field syntax `(. srv FieldName)` requires the field name
;; to be known at read time. To look up fields by a runtime string,
;; reach for `reflect`: `reflect.ValueOf` yields a `reflect.Value`;
;; `.Elem` dereferences a pointer; `.FieldByName` walks by string;
;; `.Interface` unwraps the value back to a regular Go interface.

(ns main.core)

(defn -main []
  (let [srv  (new net:http.Server)
        _    (set! (. srv Addr) "localhost:8080")
        elem (.Elem (reflect.ValueOf srv))]
    (doseq [name ["Addr" "MaxHeaderBytes" "Handler"]]
      (let [f (.FieldByName elem name)]
        (println name "->" (.Interface f))))))
