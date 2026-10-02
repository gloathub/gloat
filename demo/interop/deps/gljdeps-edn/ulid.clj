(ns main.core)

(def ulid-pkg 'github.com:oklog:ulid:v2)

(defn -main []
  (let [id           (github.com:oklog:ulid:v2.Make)
        s            (.String id)
        [parsed err] (github.com:oklog:ulid:v2.Parse s)]
    (println "package:" ulid-pkg)
    (println "make   :" s)
    (println "parsed :" (.String parsed))
    (println "err    :" err)))
