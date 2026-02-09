;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript v0 runtime for Gloat
;; Re-exports everything from std and dwim, plus map-parse helper

(ns ys.v0
  (:require
    [ys.fs]
    [ys.std :refer :all]
    [ys.dwim :refer :all]))

(defn map-parse [args]
  (mapv
    (fn [s]
      (if (re-matches #"^[+-]?[0-9]+\.?[0-9]*([eE][+-]?[0-9]+)?$" s)
        (read-string s)
        s))
    args))
