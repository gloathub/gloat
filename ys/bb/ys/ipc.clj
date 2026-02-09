;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; IPC (Inter-Process Communication) library for Babashka
;; Delegates to babashka.process

(ns ys.ipc
  (:require [babashka.process :as process]))

(defn sh [& xs]
  (apply process/sh xs))

(defn shell [& xs]
  (apply process/shell xs))

(defn process [& xs]
  (apply process/process xs))
