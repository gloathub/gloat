(ns picker.core)

(defn init-model []
  {:choices ["milk" "eggs" "bread" "butter" "cheese"
             "orange juice" "pasta" "olive oil" "tomatoes" "apples"]
   :cursor 0
   :selected #{}
   :done false})

(defn update-model [model key-str]
  (let [cursor (:cursor model)
        n (count (:choices model))
        selected (:selected model)]
    (cond
      (or (= key-str "q") (= key-str "ctrl+c"))
      nil

      (= key-str "d")
      (assoc model :done true)

      (or (= key-str "k") (= key-str "up"))
      (assoc model :cursor (max 0 (dec cursor)))

      (or (= key-str "j") (= key-str "down"))
      (assoc model :cursor (min (dec n) (inc cursor)))

      (or (= key-str "enter") (= key-str " "))
      (if (contains? selected cursor)
        (assoc model :selected (disj selected cursor))
        (assoc model :selected (conj selected cursor)))

      :else
      model)))

(defn format-result [model]
  (when (:done model)
    (let [choices (:choices model)
          selected (:selected model)
          items (vec (map #(nth choices %) (sort selected)))]
      (if (empty? items)
        "We don't need to buy anything."
        (let [n (count items)
              last-item (nth items (dec n))
              front (subvec items 0 (dec n))]
          (if (empty? front)
            (str "We need to buy " last-item ".")
            (str "We need to buy "
                 (apply str (interpose ", " front))
                 " and " last-item ".")))))))

(defn view-model [model]
  (let [choices (:choices model)
        cursor (:cursor model)
        selected (:selected model)]
    (str "What should we buy at the market?\n\n"
         (apply str
           (map-indexed
             (fn [i choice]
               (let [cursor-str (if (= i cursor) "> " "  ")
                     checked-str (if (contains? selected i) "[x] " "[ ] ")]
                 (str cursor-str checked-str choice "\n")))
             choices))
         "\nPress d when done, q to quit.\n")))
