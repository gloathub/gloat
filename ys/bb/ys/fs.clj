(ns ys.fs
  (:require
    [babashka.fs :as fs]
    [clojure.java.io :as io]
    [clojure.string :as str]))

;;------------------------------------------------------------------------------
;; Helper functions
;;------------------------------------------------------------------------------

(defn- multi [f]
  "Wrap single-path function to support multiple paths"
  (fn [& paths]
    (if (= 1 (count paths))
      (f (first paths))
      (map f paths))))

;;------------------------------------------------------------------------------
;; Predicate functions (single char aliases: d e f l r s w x z)
;;------------------------------------------------------------------------------

(defn abs? [path]
  "True if path is absolute"
  (fs/absolute? path))

(defn dir? [path]
  "True if path is a directory"
  (fs/directory? path))

(defn empty? [path]
  "True if file size is 0 or directory is empty"
  (cond
    (fs/directory? path) (clojure.core/empty? (fs/list-dir path))
    (fs/regular-file? path) (zero? (fs/size path))
    :else false))

(defn exec? [path]
  "True if path is executable"
  (fs/executable? path))

(defn exists? [path]
  "True if path exists"
  (fs/exists? path))

(defn file? [path]
  "True if path is a regular file"
  (fs/regular-file? path))

(defn link? [path]
  "True if path is a symbolic link"
  (fs/sym-link? path))

(defn read? [path]
  "True if path is readable"
  (fs/readable? path))

(defn rel? [path]
  "True if path is relative"
  (fs/relative? path))

(defn size? [path]
  "True if file size > 0"
  (and (fs/exists? path) (> (fs/size path) 0)))

(defn write? [path]
  "True if path is writable"
  (fs/writable? path))

;; Single-char aliases
(def d (multi dir?))
(def e (multi exists?))
(def f (multi file?))
(def l (multi link?))
(def r (multi read?))
(def s (multi size?))
(def w (multi write?))
(def x (multi exec?))
(def z (multi empty?))

;;------------------------------------------------------------------------------
;; Path manipulation functions
;;------------------------------------------------------------------------------

(defn abs [path]
  "Return absolute path"
  (str (fs/absolutize path)))

(defn basename [path]
  "Return basename of path (follows symlinks)"
  (str (fs/file-name (fs/canonicalize path))))

(defn ctime [path]
  "Return creation time in milliseconds (same as mtime in Unix)"
  (when (fs/exists? path)
    (.toMillis (fs/creation-time path))))

(defn mtime [path]
  "Return modification time in milliseconds"
  (when (fs/exists? path)
    (.toMillis (fs/last-modified-time path))))

(defn cwd []
  "Return current working directory"
  (str (fs/cwd)))

(defn dirname [path]
  "Return directory name of path (follows symlinks)"
  (str (fs/parent (fs/canonicalize path))))

(defn filename [path]
  "Return filename without extension"
  (let [base (str (fs/file-name path))
        ext (fs/extension path)]
    (if (str/blank? ext)
      base
      (subs base 0 (- (count base) (count ext))))))

(defn find [path & patterns]
  "Walk directory tree and return matching paths"
  (let [all-files (map str (file-seq (io/file path)))]
    (if (clojure.core/empty? patterns)
      all-files
      (filter (fn [p]
                (some #(str/includes? p %) patterns))
              all-files))))

(defn glob [pattern]
  "Return paths matching glob pattern"
  (mapv str (fs/glob "." pattern)))

(defn ls [path]
  "List directory contents"
  (mapv #(str (fs/file-name %)) (fs/list-dir path)))

(defn path [p]
  "Return canonical path (follows symlinks)"
  (str (fs/canonicalize p)))

(defn readlink [path]
  "Read symbolic link target"
  (when (fs/sym-link? path)
    (str (fs/read-link path))))

(defn rel [path]
  "Return path relative to current directory"
  (str (fs/relativize (fs/cwd) path)))

(defn which [name]
  "Find executable in PATH"
  (when-let [result (fs/which name)]
    (str result)))

;;------------------------------------------------------------------------------
;; File operation functions
;;------------------------------------------------------------------------------

(defn cp [src dst]
  "Copy file from src to dst"
  (fs/copy src dst {:replace-existing true})
  nil)

(defn cp-r [src dst]
  "Recursively copy directory"
  (fs/copy-tree src dst {:replace-existing true})
  nil)

(defn mkdir [path]
  "Create directory"
  (fs/create-dir path)
  nil)

(defn mkdir-p [path]
  "Create directory and parents"
  (fs/create-dirs path)
  nil)

(defn mv [src dst]
  "Move/rename file or directory"
  (fs/move src dst {:replace-existing true})
  nil)

(defn rm [path]
  "Remove file or empty directory"
  (fs/delete path)
  nil)

(defn rm-f [path]
  "Remove file, ignore errors"
  (fs/delete-if-exists path)
  nil)

(defn rm-r [path]
  "Remove directory recursively"
  (fs/delete-tree path)
  nil)

(defn rmdir [path]
  "Remove empty directory"
  (fs/delete path)
  nil)

(defn touch [path]
  "Update file timestamp or create if doesn't exist"
  (if (fs/exists? path)
    (fs/set-last-modified-time path (java.time.Instant/now))
    (spit path ""))
  nil)
