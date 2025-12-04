;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript v0 runtime for Gloat
;; Re-exports everything from std and dwim, plus map-parse helper

(ns ys.v0
  (:require
    [ys.std :refer :all]
    [ys.dwim :refer :all]))

(defn map-parse [args]
  (mapv
    (fn [s] (let [x (read-string s)] (if (number? x) x s)))
    args))
