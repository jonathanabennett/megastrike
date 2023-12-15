(ns megastrike.hexagons.hex
  (:require
   [clojure.math :as math]))

(defn hexagon
  ([q r]
   (hexagon q r (* (+ q r) -1)))
  ([q r s]
   (when (= (* (+ q r) -1) s)
      {:q q :r r :s s})))

(defn hex-from-offset
  ([col row]
  (let [q col
        r (int (- row (math/floor (/ (+ col (* (mod (abs col) 2) -1)) 2))))
        s (int (* (+ q r) -1))]
    (hexagon q r s)))
  ([offset]
   (hex-from-offset (:x offset) (:y offset))))

(defn offset-from-hex
  [hex]
  (let [row (int (+ (:r hex) (math/floor (/ (+ (:q hex) (* (mod (abs (:q hex)) 2) -1)) 2))))
        col (:q hex)]
    {:x col :y row}))

(defn same-hex
  [hex1 hex2]
  (and (= (:q hex1) (:q hex2))
       (= (:r hex1) (:r hex2))
       (= (:s hex1) (:s hex2))))

(defn hex-addition
  "Use Cartesian addition to add two hexagons together."
  [hex1 hex2]
  (hexagon (+ (:q hex1) (:q hex2)) (+ (:r hex1) (:r hex2)) (+ (:s hex1) (:s hex2))))

(defn hex-subtraction
  "Use Cartesian subtraction to add two hexagons together."
  [hex1 hex2]
  (hexagon (- (:q hex1) (:q hex2)) (- (:r hex1) (:r hex2)) (- (:s hex1) (:s hex2))))

(defn hex-multiplication
  "Use Cartesian multiplication to multipy a hex by a value x."
  [hex x]
  (hexagon (* (:q hex) x) (* (:r hex) x) (* (:s hex) x)))

(defn hex-distance
  [hex1 hex2]
  (let [length (hex-subtraction hex1 hex2)]
    (/ (+ (abs (:q length)) (abs (:r length)) (abs (:s length))) 2)))

(def hex-ordinals (list {:q 1  :r 0  :s -1}
                        {:q 1  :r -1 :s 0}
                        {:q 0  :r -1 :s 1}
                        {:q -1 :r 0  :s 1}
                        {:q -1 :r 0  :s 1}
                        {:q 0  :r 1  :s -1}))

(defn hex-direction
  "Returns the coordinate transformation to select a hex in a given direction"
  [direction]
  (let [dir (mod direction 6)]
    (nth hex-ordinals dir)))

(defn hex-neighbor
  [hex direction]
  (hex-addition hex (hex-direction direction)))

(defn create-layout
  []
  {:hex-to-pixel-matrix [(/ 3.0 2.0) 0 (/ (math/sqrt 3.0) 2.0) (math/sqrt 3.0)]
   :pixel-to-hex-matrix [(/ 2.0 3.0) 0 (/ -1.0 3.0) (/ (math/sqrt 3.0) 3.0)]
   :start-angle 0
   :x-size 84
   :y-size 72
   :x-origin 84
   :y-origin 65})

(defn hex-to-pixel
  [hex layout]
  (let [htp (:hex-to-pixel-matrix layout)]
    {:x (+ (* (+ (* (get htp 0) (:q hex))
                 (* (get htp 1) (:r hex)))
              (:x-size layout))
           (:x-origin layout))
     :y (+ (* (+ (* (get htp 2) (:q hex))
                 (* (get htp 3) (:r hex)))
              (:y-size layout))
           (:y-origin layout))}))

(defn hex-round
  [q r s]
  (let [q-int (math/round q)
        r-int (math/round r)
        s-int (math/round s)
        q-diff (abs (- q q-int))
        r-diff (abs (- r r-int))
        s-diff (abs (- s s-int))]
    (cond
      (and (> q-diff r-diff)
            (> q-diff s-diff))
       (hexagon (* (+ r-int s-int) -1) r-int s-int)
      (and (> r-diff s-diff)
            (> r-diff q-diff))
       (hexagon q-int (* (+ q-int s-int) -1) s-int)
      :else (hexagon q-int r-int (* (+ q-int r-int) -1)))))

(defn pixel-to-hex
  [pt layout]
  (let [pth (:pixel-to-hex-matrix layout)
        modified-point {:x (/ (- (:x pt)
                                 (:x-origin layout))
                              (:x-size layout))
                        :y (/ (- (:y pt)
                                 (:y-origin layout))
                              (:y-size layout))}
        q (+ (* (:x modified-point) (get pth 0))
             (* (:y modified-point) (get pth 1)))
        r (+ (* (:x modified-point) (get pth 2))
             (* (:y modified-point) (get pth 3)))]
    (hex-round q r (* (+ q r) -1))))

(defn find-hex-corner
  [center corner layout]
  (let [angle (* 2.0 math/PI (/ (+ (:start-angle layout) corner) 6))]
    [(+ (* (:x-size layout)
              (math/cos angle))
           (:x center))
     (+ (* (:y-size layout)
              (math/sin angle))
           (:y center))]))

(defn hex-points
  [hex layout]
  (let [center (hex-to-pixel hex layout)]
    (flatten (map #(find-hex-corner center % layout) (list 0 1 2 3 4 5)))))

(defn linear-interpolation
  [a b step]
  (+ a (* (- b a) step)))

(defn hex-lerp
  [hex1 hex2 step]
  {:q (linear-interpolation (:q hex1) (:q hex2) step)
   :r (linear-interpolation (:r hex1) (:r hex2) step)
   :s (linear-interpolation (:s hex1) (:s hex2) step)})

(defn hex-line
  [hex1 hex2]
  (let [distance (hex-distance hex1 hex2)
        step (/ 1.0 (max distance 1))]
    [{:q 0 :r 0 :s 0}]))
