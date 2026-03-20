;; deps.clj - Dependency graph output for gloat -Xdeps
;;
;; Operates on the live dependency graph from prune/build-graph,
;; producing tree, sorted-tree, or flat-list output.

(ns deps
  (:require
   [clojure.string :as str]))

(def ^:dynamic *strip-ns* false)

(defn format-key
  "Format a \"ns/fn\" key, stripping namespace prefix if *strip-ns*."
  [k]
  (if *strip-ns*
    (second (str/split k #"/" 2))
    k))

(defn build-dep-tree
  "Build nested tree from the :edges map (from prune/build-graph).
   Uses ancestor set for cycle detection.
   Returns sorted-map of \"ns/fn\" -> subtree."
  [edges node ancestors]
  (let [refs (get edges node)
        children (when refs
                   (let [keys (map (fn [[ns fn-name]] (str ns "/" fn-name))
                                   refs)]
                     (into (sorted-map)
                           (for [child-key keys
                                 :when (not (contains? ancestors child-key))
                                 :let [[ns fn-name]
                                       (str/split child-key #"/" 2)
                                       child-node [ns fn-name]
                                       sub (build-dep-tree
                                            edges child-node
                                            (conj ancestors child-key))]]
                             [child-key sub]))))]
    (when (and children (seq children))
      children)))

(defn emit-yaml-tree
  "Recursively emit YAML with indentation."
  [tree indent]
  (let [prefix (apply str (repeat indent "  "))]
    (doseq [[k subtree] tree]
      (if (and subtree (seq subtree))
        (do
          (println (str prefix (format-key k) ":"))
          (emit-yaml-tree subtree (inc indent)))
        (println (str prefix (format-key k) ":"))))))

(defn find-user-roots
  "Find root functions: edge keys whose namespace is NOT
   clojure.*, ys.*, or yamlscript.*."
  [edges]
  (filter (fn [[ns _]]
            (not (or (str/starts-with? ns "clojure.")
                     (str/starts-with? ns "ys.")
                     (str/starts-with? ns "yamlscript."))))
          (keys edges)))

(defn emit-deps-tree
  "Build tree with top-level keys sorted, inner levels in natural order."
  [edges roots]
  (let [root-keys (sort (map (fn [[ns fn-name]] (str ns "/" fn-name)) roots))]
    (doseq [root-key root-keys]
      (let [[ns fn-name] (str/split root-key #"/" 2)
            node [ns fn-name]
            subtree (build-dep-tree edges node #{root-key})]
        (println (str (format-key root-key) ":"))
        (when (and subtree (seq subtree))
          (emit-yaml-tree subtree 1))))))

(defn tree-walk-unique
  "Pre-order DFS collecting unique \"ns/fn\" strings."
  [tree seen result]
  (reduce (fn [[seen result] [k subtree]]
            (if (contains? seen k)
              [seen result]
              (let [seen (conj seen k)
                    result (conj result k)]
                (if (and subtree (seq subtree))
                  (tree-walk-unique subtree seen result)
                  [seen result]))))
          [seen result]
          tree))

(defn emit-deps-list
  "Build tree, walk it, print sorted unique flat list."
  [edges roots]
  (let [root-keys (sort (map (fn [[ns fn-name]] (str ns "/" fn-name)) roots))
        all-entries (atom #{})
        _ (doseq [root-key root-keys]
            (swap! all-entries conj root-key)
            (let [[ns fn-name] (str/split root-key #"/" 2)
                  node [ns fn-name]
                  subtree (build-dep-tree edges node #{root-key})]
              (when subtree
                (let [[_ entries] (tree-walk-unique subtree #{} [])]
                  (swap! all-entries into entries)))))]
    (doseq [entry (sort @all-entries)]
      (println (format-key entry)))))

(defn emit-deps
  "Dispatcher for deps output modes: :tree, :list, :tree-sort.
   When quiet? is true, strip namespace prefixes from output."
  [mode edges roots quiet?]
  (binding [*strip-ns* (boolean quiet?)]
    (case mode
      :tree (emit-deps-tree edges roots)
      :list (emit-deps-list edges roots)))
  (flush))
