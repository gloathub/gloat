;; open.clj - Cross-platform browser opening utility

(ns open
  (:require
   [babashka.process :as process]))

(defn open-browser
  "Open a URL in the default browser.
   Uses xdg-open on Linux, open on macOS."
  [url]
  (let [cmd (case (System/getProperty "os.name")
              "Mac OS X" "open"
              "xdg-open")]
    (process/process [cmd url])))
