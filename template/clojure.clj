(ns NAMESPACE
  (:require [ys.v0 :refer :all])
  (:refer-clojure :exclude [atom die print read replace reverse set]))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])
(def ^:dynamic ENV {})
(def ^:dynamic NS nil)
(def ^:dynamic RUN {})
(def ^:dynamic FILE "SOURCE-FILE")
(def ^:dynamic DIR "SOURCE-DIR")
(def ^:dynamic CWD "")

BODY
MAIN-FN
  