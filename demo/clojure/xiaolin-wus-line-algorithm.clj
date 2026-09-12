;; https://rosettacode.org/wiki/Xiaolin_Wu%27s_line_algorithm#Clojure

(ns main.core)

(defn abs [x]
  (if (< x 0) (- x) x))

(defn fpart [x]
  (- x (long x)))

(defn rfpart [x]
  (- 1 (fpart x)))

(defn plot [x y c]
  (when (not= c 0.0)
    (println (format "plot %d %d %.1f" (long x) (long y) (double c)))))

(defn draw-line [x0 y0 x1 y1]
  (let [[plot-fn x0 x1 y0 y1]
        (if (> (abs (- y1 y0)) (abs (- x1 x0)))
          [(fn [x y c] (plot y x c)) y0 y1 x0 x1]
          [plot x0 x1 y0 y1])

        [x0 x1 y0 y1]
        (if (> x0 x1)
          [x1 x0 y1 y0]
          [x0 x1 y0 y1])

        dx (- x1 x0)
        dy (- y1 y0)
        gradient (/ dy dx)

        [intery xends]
        (loop [intery nil
               xends []
               coords [[x0 y0] [x1 y1]]]
          (if (empty? coords)
            [intery xends]
            (let [[[x y] & rest-coords] coords
                  xend (long (+ x 0.5))
                  yend (+ y (* gradient (- xend x)))
                  xgap (rfpart (+ x 0.5))
                  x-pixel xend
                  y-pixel (long yend)
                  new-intery (if (nil? intery) (+ yend gradient) intery)]
              (plot-fn x-pixel y-pixel (* (rfpart yend) xgap))
              (plot-fn x-pixel (inc y-pixel) (* (fpart yend) xgap))
              (recur new-intery
                     (conj xends x-pixel)
                     rest-coords))))]

    (loop [intery intery
           x-vals (range (inc (first xends)) (second xends))]
      (when (seq x-vals)
        (let [x (first x-vals)
              y (long intery)]
          (plot-fn x y (rfpart intery))
          (plot-fn x (inc y) (fpart intery))
          (recur (+ intery gradient) (rest x-vals)))))))

(defn -main [& args]
  (let [x0 (parse-long (or (nth args 0 nil) "0"))
        y0 (parse-long (or (nth args 1 nil) "1"))
        x1 (parse-long (or (nth args 2 nil) "10"))
        y1 (parse-long (or (nth args 3 nil) "2"))]
    (draw-line x0 y0 x1 y1)))
