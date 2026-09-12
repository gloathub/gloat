;; Calling java.lang.Thread/sleep.
;;
;; gloat rewrites `Thread/sleep` to glojure's javacompat bridge, which forwards
;; to gojava's thread package; the implementation wraps Go's `time.Sleep`. Only
;; Thread/sleep is supported today: the full Java Thread API (start, join,
;; interrupt, names, uncaught handlers) has no clean goroutine analogue and is
;; intentionally out of scope.
;;
;; Both the one-argument (millis) and two-argument (millis, nanos) overloads
;; resolve through the same variadic Go signature. Negative arguments or
;; out-of-range nanoseconds panic, matching the JVM's IllegalArgumentException.

(ns main.core)

(defn -main []
  ;; Basic millisecond sleep
  (println "before sleep")
  (Thread/sleep 10)
  (println "after  sleep")

  ;; Two-argument overload: millis + nanos
  (Thread/sleep 1 500000)
  (println "after  sleep with nanos")

  ;; Measure that the sleep actually blocked
  (let [t0 (System/currentTimeMillis)
        _  (Thread/sleep 25)
        dt (- (System/currentTimeMillis) t0)]
    (println "elapsed ms >= 25:" (>= dt 25)))

  ;; Fully qualified form also resolves
  (java.lang.Thread/sleep 1)
  (println "fq sleep ok"))
