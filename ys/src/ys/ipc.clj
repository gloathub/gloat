;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; IPC (Inter-Process Communication) library for Gloat
;; Provides cross-platform process execution using Go's os/exec package

(ns ys.ipc
  (:require [clojure.string :as str]))

;;------------------------------------------------------------------------------
;; Helper functions
;;------------------------------------------------------------------------------

(defn- bytes-to-str [b]
  "Convert Go []byte to string"
  (fmt.Sprintf "%s" b))

;;------------------------------------------------------------------------------
;; Process execution functions
;;------------------------------------------------------------------------------

(defn sh [& args]
  "Execute command directly (first arg is command, rest are arguments)"
  (if (empty? args)
    {:exit 1 :out "" :err "No command specified"}
    (let [[cmd-name & cmd-args] args
          cmd (apply os:exec.Command cmd-name (vec cmd-args))
          [stdout-pipe stdout-err] (.StdoutPipe cmd)
          [stderr-pipe stderr-err] (.StderrPipe cmd)]
      (if (or (not (nil? stdout-err)) (not (nil? stderr-err)))
        {:exit 1 :out "" :err (str "Failed to create pipes")}
        (let [start-err (.Start cmd)]
          (if (not (nil? start-err))
            {:exit 1 :out "" :err (str "Failed to start: " start-err)}
            (let [[stdout-bytes stdout-read-err] (io.ReadAll stdout-pipe)
                  [stderr-bytes stderr-read-err] (io.ReadAll stderr-pipe)
                  wait-err (.Wait cmd)
                  exit-code (if (nil? wait-err)
                              0
                              (.ExitCode (.ProcessState cmd)))]
              {:exit exit-code
               :out (if (nil? stdout-read-err)
                      (bytes-to-str stdout-bytes)
                      "")
               :err (if (nil? stderr-read-err)
                      (bytes-to-str stderr-bytes)
                      "")})))))))

(defn shell [& args]
  "Execute command via /bin/sh -c (joins args into single command string)"
  (let [cmd-str (str/join " " args)]
    (sh "/bin/sh" "-c" cmd-str)))

(defn process [& args]
  "Execute command via /bin/sh -c (joins args into single command string)"
  (apply shell args))
