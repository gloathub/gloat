(ns uuid-test
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "random-version"  (let [u (UUID/randomUUID)] (println (.version u)))
      "random-variant"  (let [u (UUID/randomUUID)] (println (.variant u)))
      "random-format"   (let [u (UUID/randomUUID)]
                          (println (boolean (re-matches
                                              #"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                                              (.toString u)))))
      "from-string"     (let [u (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")]
                          (println (.toString u)))
      "msb"             (let [u (UUID/fromString "00000000-0000-002a-0000-000000000000")]
                          (println (.getMostSignificantBits u)))
      "lsb"             (let [u (UUID/fromString "00000000-0000-0000-0000-00000000002a")]
                          (println (.getLeastSignificantBits u)))
      "named-version"   (let [u (UUID/nameUUIDFromBytes (.getBytes "hello"))]
                          (println (.version u)))
      "named-string"    (let [u (UUID/nameUUIDFromBytes (.getBytes "hello"))]
                          (println (.toString u)))
      "ctor-string"     (let [u (UUID. 42 99)] (println (.toString u)))
      "ctor-msb"        (let [u (UUID. 42 99)] (println (.getMostSignificantBits u)))
      "ctor-lsb"        (let [u (UUID. 42 99)] (println (.getLeastSignificantBits u)))
      "equals-true"     (println (.equals (UUID. 1 2) (UUID. 1 2)))
      "equals-false"    (println (.equals (UUID. 1 2) (UUID. 1 3)))
      "compare-eq"      (println (.compareTo (UUID. 1 2) (UUID. 1 2)))
      "compare-lt"      (println (.compareTo (UUID. 1 2) (UUID. 1 3)))
      "compare-gt"      (println (.compareTo (UUID. 2 0) (UUID. 1 9)))
      "hashcode-stable" (println (= (.hashCode (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef"))
                                    (.hashCode (UUID/fromString "01234567-89ab-cdef-0123-456789abcdef"))))
      "fq-random"       (println (boolean (re-find #"-" (.toString (java.util.UUID/randomUUID)))))
      "fq-fromstring"   (let [u (java.util.UUID/fromString "01234567-89ab-cdef-0123-456789abcdef")]
                          (println (.toString u)))
      (println "usage: uuid <op>"))))
