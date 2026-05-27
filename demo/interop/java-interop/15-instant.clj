;; Calling java.time.Instant static members and instance methods.
;;
;; Same three-layer flow as the java.lang.* classes: gloat rewrites each
;; `Instant/*` symbol to glojure's javacompat bridge, which forwards to
;; gojava's typed Go port. Instance methods (toString, plusMillis,
;; compareTo, ...) reach through reflection on the *Instant receiver.
;; The Go type implements fmt.Stringer so (str i) and println yield the
;; canonical ISO-8601 form.
;;
;; java.time.Instant has no public constructor in the JVM (only static
;; factories), so there is no `(Instant. ...)` ctor sugar here.

(ns main.core)

(defn -main []
  ;; EPOCH constant
  (println "EPOCH:" (.toString Instant/EPOCH))

  ;; Current moment in UTC
  (let [now (Instant/now)]
    (println "now:" (.toString now))
    (println "now epoch second:" (.getEpochSecond now)))

  ;; Parsing ISO-8601
  (let [a (Instant/parse "2007-12-03T10:15:30Z")
        b (Instant/parse "2007-12-03T10:15:30.500Z")
        c (Instant/parse "2007-12-03T10:15:30.500000123Z")]
    (println "no fraction:"   (.toString a))
    (println "millis:"        (.toString b))
    (println "nanos:"         (.toString c)))

  ;; Factory methods
  (println "ofEpochSecond:"        (.toString (Instant/ofEpochSecond 1196676930)))
  (println "ofEpochSecond 2-arg:"  (.toString (Instant/ofEpochSecond 0 1500000000)))
  (println "ofEpochMilli:"         (.toString (Instant/ofEpochMilli 1196676930500)))

  ;; Conversion back
  (let [i (Instant/parse "2007-12-03T10:15:30.500Z")]
    (println "toEpochMilli:"   (.toEpochMilli i))
    (println "getEpochSecond:" (.getEpochSecond i))
    (println "getNano:"        (.getNano i)))

  ;; Arithmetic
  (let [i (Instant/parse "2007-12-03T10:15:30Z")]
    (println "plus 5 seconds:" (.toString (.plusSeconds i 5)))
    (println "plus 1500 millis:" (.toString (.plusMillis i 1500)))
    (println "plus 1 nano:" (.toString (.plusNanos i 1)))
    (println "minus 5 seconds:" (.toString (.minusSeconds i 5))))

  ;; Comparison
  (let [a (Instant/parse "2007-12-03T10:15:30Z")
        b (Instant/parse "2007-12-03T10:15:31Z")]
    (println "isBefore:"  (.isBefore a b))
    (println "isAfter:"   (.isAfter b a))
    (println "compareTo:" (.compareTo a b)))

  ;; Fully qualified form also resolves
  (println "fq parse:" (.toString (java.time.Instant/parse "2007-12-03T10:15:30Z"))))
