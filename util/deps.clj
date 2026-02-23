#!/usr/bin/env bb

;; deps.clj - Print tree-style dependency graph for clojure.core functions
;;
;; Usage: bb deps.clj fn1 fn2 ...

(require '[clojure.java.io :as io]
         '[clojure.string :as str])

(def yaml-file "build/clojure-core.yaml")

(defn parse-yaml
  "Minimal YAML parser for clojure-core.yaml (mapping of keys to lists)."
  [text]
  (let [lines (str/split-lines text)]
    (loop [i 0
           current-key nil
           result {}]
      (if (>= i (count lines))
        result
        (let [line (nth lines i)]
          (cond
            ;; Blank line or comment
            (or (str/blank? line) (str/starts-with? line "#"))
            (recur (inc i) current-key result)

            ;; Sequence item (starts with "- ")
            (str/starts-with? line "- ")
            (let [val (str/trim (subs line 2))
                  val (if (and (str/starts-with? val "'")
                              (str/ends-with? val "'"))
                        (subs val 1 (dec (count val)))
                        val)]
              (recur (inc i)
                     current-key
                     (update result current-key (fnil conj []) val)))

            ;; Key line (ends with ":")
            :else
            (let [key-str (str/trim line)
                  key-str (if (str/ends-with? key-str ":")
                            (subs key-str 0 (dec (count key-str)))
                            key-str)
                  key-str (if (and (str/starts-with? key-str "'")
                                  (str/ends-with? key-str "'"))
                            (subs key-str 1 (dec (count key-str)))
                            key-str)]
              (recur (inc i) key-str result))))))))

(defn clean-deps
  "Filter and clean a dependency list: skip ./S, self-refs, strip /M suffix."
  [deps fn-name]
  (->> deps
       (remove #(= % "./S"))
       (remove #(str/ends-with? % "/S"))
       (remove #(= % fn-name))
       (map #(if (str/ends-with? % "/M")
               (subs % 0 (- (count %) 2))
               %))
       distinct
       sort
       vec))

(defn collect-all-deps
  "Collect all unique deps reachable from fn-name (transitive closure)."
  [graph fn-name visited]
  (if (contains? visited fn-name)
    visited
    (let [visited (conj visited fn-name)
          raw-deps (get graph fn-name)
          deps (if raw-deps (clean-deps raw-deps fn-name) [])]
      (reduce #(collect-all-deps graph %2 %1) visited deps))))

(defn build-tree
  "Build nested tree for fn-name. ancestors is the set of names in the
   current path from root to here (used to avoid cycles)."
  [graph fn-name ancestors]
  (let [raw-deps (get graph fn-name)
        deps (if raw-deps (clean-deps raw-deps fn-name) [])
        ancestors' (conj ancestors fn-name)]
    (if (empty? deps)
      nil ;; leaf with no entry in graph
      (vec
       (for [dep deps
             :when (not (contains? ancestors' dep))]
         (let [sub (build-tree graph dep ancestors')]
           (if sub
             {dep sub}
             dep)))))))

(defn emit-yaml
  "Emit tree as YAML with proper indentation."
  [tree indent]
  (let [prefix (apply str (repeat indent "  "))]
    (doseq [item tree]
      (cond
        (string? item)
        (println (str prefix "- " item))

        (map? item)
        (let [[k v] (first item)]
          (println (str prefix "- " k ":"))
          (emit-yaml v (inc indent)))))))

(defn -main [& args]
  (when (empty? args)
    (println "Usage: bb deps.clj fn1 fn2 ...")
    (System/exit 1))
  (let [graph (parse-yaml (slurp yaml-file))
        unique? (System/getenv "UNIQUE")]
    (if unique?
      (let [all-deps (reduce #(collect-all-deps graph %2 %1) #{} args)
            sorted (sort all-deps)]
        (doseq [dep sorted]
          (println dep)))
      (doseq [fn-name args]
        (let [tree (build-tree graph fn-name #{})]
          (print (str fn-name ":"))
          (if tree
            (do (println)
                (emit-yaml tree 0))
            (println " []")))))))

(apply -main *command-line-args*)
