;; https://rosettacode.org/wiki/99_bottles_of_beer#Clojure

(ns main.core)

(defn paragraph [num]
  (str num " bottles of beer on the wall\n"
       num " bottles of beer\n"
       "Take one down, pass it around\n"
       (dec num) " bottles of beer on the wall.\n"))

(defn lyrics [num]
  (let [numbers (range num 0 -1)
        paragraphs (map paragraph numbers)]
    (clojure.string/join "\n" paragraphs)))

(defn -main [& args]
  (let [n (parse-long (or (first args) "99"))]
    (println (lyrics n))))
