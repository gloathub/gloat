;; Calling Go package functions.
;;
;; The Go form `fmt.Sprintf(...)` becomes `(fmt.Sprintf ...)` in Glojure.
;; The package name and the symbol stay joined by a dot; arguments
;; follow as plain Glojure values. Single-segment Go packages (`fmt`,
;; `strings`) need no colonified path.

(ns main.core)

(defn -main []
  (println (fmt.Sprintf "Hello, %s!" "world"))
  (println (strings.HasPrefix "foobar" "foo"))
  (let [parts (strings.Split "alpha,beta,gamma" ",")]
    (println (strings.Join parts " | "))))
