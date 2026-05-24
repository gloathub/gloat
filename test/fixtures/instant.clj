(ns instant-test
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(defn -main [& args]
  (let [op (first args)]
    (case op
      "epoch-string"   (println (.toString Instant/EPOCH))
      "epoch-seconds"  (println (.getEpochSecond Instant/EPOCH))
      "epoch-nano"     (println (.getNano Instant/EPOCH))
      "now-shape"      (let [s (.toString (Instant/now))]
                         (println (boolean (re-matches
                                             #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3}|\.\d{6}|\.\d{9})?Z"
                                             s))))
      "parse-no-frac"  (println (.toString (Instant/parse "2007-12-03T10:15:30Z")))
      "parse-millis"   (println (.toString (Instant/parse "2007-12-03T10:15:30.500Z")))
      "parse-nanos"    (println (.toString (Instant/parse "2007-12-03T10:15:30.500000123Z")))
      "epoch-second-1" (println (.toString (Instant/ofEpochSecond 1196676930)))
      "epoch-second-2" (println (.toString (Instant/ofEpochSecond 0 1500000000)))
      "epoch-milli"    (println (.toString (Instant/ofEpochMilli 1196676930500)))
      "to-epoch-milli" (println (.toEpochMilli (Instant/parse "2007-12-03T10:15:30.500Z")))
      "get-seconds"    (println (.getEpochSecond (Instant/parse "2007-12-03T10:15:30.500Z")))
      "get-nano"       (println (.getNano (Instant/parse "2007-12-03T10:15:30.500Z")))
      "plus-seconds"   (println (.toString (.plusSeconds (Instant/parse "2007-12-03T10:15:30Z") 5)))
      "plus-millis"    (println (.toString (.plusMillis (Instant/parse "2007-12-03T10:15:30.500Z") 1500)))
      "plus-nanos"     (println (.toString (.plusNanos (Instant/parse "2007-12-03T10:15:30Z") 1)))
      "minus-seconds"  (println (.toString (.minusSeconds (Instant/parse "2007-12-03T10:15:30Z") 5)))
      "minus-millis"   (println (.toString (.minusMillis (Instant/parse "2007-12-03T10:15:30.500Z") 250)))
      "is-before"      (println (.isBefore (Instant/parse "2007-12-03T10:15:30Z")
                                           (Instant/parse "2007-12-03T10:15:31Z")))
      "is-after"       (println (.isAfter (Instant/parse "2007-12-03T10:15:31Z")
                                          (Instant/parse "2007-12-03T10:15:30Z")))
      "compare-eq"     (println (.compareTo (Instant/parse "2007-12-03T10:15:30Z")
                                            (Instant/parse "2007-12-03T10:15:30Z")))
      "compare-lt"     (println (.compareTo (Instant/parse "2007-12-03T10:15:30Z")
                                            (Instant/parse "2007-12-03T10:15:30.000000001Z")))
      "compare-gt"     (println (.compareTo (Instant/parse "2007-12-03T10:15:31Z")
                                            (Instant/parse "2007-12-03T10:15:30Z")))
      "equals-true"    (println (.equals (Instant/parse "2007-12-03T10:15:30Z")
                                         (Instant/parse "2007-12-03T10:15:30Z")))
      "equals-false"   (println (.equals (Instant/parse "2007-12-03T10:15:30Z")
                                         (Instant/parse "2007-12-03T10:15:31Z")))
      "hashcode-stable" (println (= (.hashCode (Instant/parse "2007-12-03T10:15:30.500Z"))
                                    (.hashCode (Instant/parse "2007-12-03T10:15:30.500Z"))))
      "fq-now-shape"   (let [s (.toString (java.time.Instant/now))]
                         (println (boolean (re-matches
                                             #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3}|\.\d{6}|\.\d{9})?Z"
                                             s))))
      "fq-parse"       (println (.toString (java.time.Instant/parse "2007-12-03T10:15:30Z"))))))
