;; Method calls on Go values.
;;
;; Two equivalent forms for invoking a method:
;;   (. obj (Method args))   ; parenthesized: clearer with multiple args
;;   (. obj Method args)     ; flat:          shorter for one or two args
;; The familiar `(.Method obj args)` shorthand also works. A zero-arg
;; method on a freshly constructed value reads naturally as
;; `(. (new T) Method)`.

(ns main.core)

(defn -main []
  (let [escaper (strings.NewReplacer "<" "&lt;" ">" "&gt;")]
    (println (. escaper (Replace "<html>")))
    (println (. escaper Replace "the <header> tag")))
  (println "Len of empty Buffer:" (. (new bytes.Buffer) Len)))
