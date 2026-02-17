(ns NAMESPACE
  (:require [ys.fs :as fs]
            [ys.http :as http]
            [ys.ipc :as ipc]
            [ys.json :as json]
            [ys.std :refer :all]
            [ys.dwim :refer :all]
            [ys.v0 :refer [map-parse]])
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
  