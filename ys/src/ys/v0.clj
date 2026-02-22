;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript v0 runtime for Gloat
;; Re-exports everything from std and dwim, plus map-parse helper

(ns ys.v0
  (:require
    [yamlscript.common]
    [yamlscript.util]
    [ys.fs]
    [ys.http]
    [ys.ipc]
    [ys.json]
    [ys.std :refer :all]
    [ys.dwim :refer :all]))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])
(def ^:dynamic ENV {})
(def ^:dynamic NS nil)
(def ^:dynamic RUN {})
(def ^:dynamic FILE "")
(def ^:dynamic DIR "")
(def ^:dynamic CWD "")

(defn map-parse [args]
  (mapv
    (fn [s]
      (if (re-matches #"^[+-]?[0-9]+\.?[0-9]*([eE][+-]?[0-9]+)?$" s)
        (read-string s)
        s))
    args))

(doseq [[sym var] (ns-publics 'ys.std)]
  (let [v (intern 'ys.v0 sym (fn [& args] (apply @var args)))]
    (alter-meta! v merge (meta var))))
(doseq [[sym var] (ns-publics 'ys.dwim)]
  (let [v (intern 'ys.v0 sym (fn [& args] (apply @var args)))]
    (alter-meta! v merge (meta var))))
