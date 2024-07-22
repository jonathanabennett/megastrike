(ns megastrike.hexagons.hex
  (:require [clojure.math :as math]))

(defn hexagon
  "Creates a Hexagon using a 3d addressing system."
  ([p q]
   (hexagon p q (* (+ p q) -1)))
  ([p q r]
   (when (= (* (+ p q) -1) r)
      {:p p :q q :r r})))

(defn hex-from-offset
  "Calculates a hex based on an 'offset' hex address. The input in in the format of [x y]."
  ([col row]
  (let [p col
        q (int (- row (math/floor (/ (+ col (* (mod (abs col) 2) -1)) 2))))
        r (int (* (+ p q) -1))]
    (hexagon p q r)))
  ([offset]
   (hex-from-offset (:x offset) (:y offset))))

(defn offset-from-hex
  "Calculates an offset address (x,y) based on a 3D address."
  [hex]
  (let [row (int (+ (:q hex) (math/floor (/ (+ (:p hex) (* (mod (abs (:p hex)) 2) -1)) 2))))
        col (:p hex)]
    {:x col :y row}))

(defn same-hex
  "A hex is the same if the p,q, and r addresses are the same."
  [hex1 hex2]
  (and (= (:p hex1) (:p hex2))
       (= (:q hex1) (:q hex2))
       (= (:r hex1) (:r hex2))))

(defn addition
  "Use Cartesian addition to add two hexagons together."
  [hex1 hex2]
  (hexagon (+ (:p hex1) (:p hex2)) (+ (:q hex1) (:q hex2)) (+ (:r hex1) (:r hex2))))

(defn subtraction
  "Use Cartesian subtraction to add two hexagons together."
  [hex1 hex2]
  (hexagon (- (:p hex1) (:p hex2)) (- (:q hex1) (:q hex2)) (- (:r hex1) (:r hex2))))

(defn multiplication
  "Use Cartesian multiplication to multipy a hex by a value x."
  [hex x]
  (hexagon (* (:p hex) x) (* (:q hex) x) (* (:r hex) x)))

(defn distance
  "The distance between two hexes using 3d addresses is half of the sum of the differences of the address hexes."
  [hex1 hex2]
  (let [length (subtraction hex1 hex2)]
    (/ (+ (abs (:p length)) (abs (:q length)) (abs (:r length))) 2)))

(def ordinals
  "Defines the neighbors in each direction."
  (list {:p 1  :q 0  :r -1} 
        {:p 1  :q -1 :r 0} 
        {:p 0  :q -1 :r 1} 
        {:p -1 :q 0  :r 1} 
        {:p -1 :q 1  :r 0} 
        {:p 0  :q 1  :r -1}))

(defn direction
  "Returns the coordinate transformation to select a hex in a given direction"
  [dir]
  (let [d (mod dir 6)]
    (nth ordinals d)))

(defn neighbor
  "The neighbor in a given direction."
  [hex dir]
  (addition hex (direction dir)))

(defn neighbors
  [hex]
  (for [i (range 6)]
    (neighbor hex i)))

(defn create-layout
  "Creates a layout. Populated with the default layout."
  []
  {:hex-to-pixel-matrix [(/ 3.0 2.0) 0 (/ (math/sqrt 3.0) 2.0) (math/sqrt 3.0)]
   :pixel-to-hex-matrix [(/ 2.0 3.0) 0 (/ -1.0 3.0) (/ (math/sqrt 3.0) 3.0)]
   :start-angle 0
   :x-size 84
   :y-size 72
   :x-origin 84
   :y-origin 65
   :scale 1.0})

(defn hex-to-pixel
  "Converts a given hex address to the pixel at the center of the hex."
  [hex layout]
  (let [htp (:hex-to-pixel-matrix layout)]
    {:x (+ (* (+ (* (get htp 0) (:p hex))
                 (* (get htp 1) (:q hex)))
              (* (:x-size layout) (:scale layout)))
           (:x-origin layout))
     :y (+ (* (+ (* (get htp 2) (:p hex))
                 (* (get htp 3) (:q hex)))
              (* (:y-size layout) (:scale layout)))
           (:y-origin layout))}))

(defn find-hex-corner
  "Calculates the corner of a hex based on the center."
  [center corner layout]
  (let [angle (* 2.0 math/PI (/ (+ (:start-angle layout) corner) 6))]
    [(+ (* (:x-size layout) (math/cos angle) (:scale layout)) 
        (:x center))
     (+ (* (:y-size layout) (math/sin angle) (:scale layout)) 
        (:y center))]))

(defn points
  "Returns a list of all the corners of a hex."
  [hex layout]
  (let [center (hex-to-pixel hex layout)]
    (flatten (map #(find-hex-corner center % layout) (list 0 1 2 3 4 5)))))

(defn round
  [{:keys [p q r]}]
  (let [p-int (math/round p)
        q-int (math/round q)
        r-int (math/round r)
        p-diff (abs (- p p-int))
        q-diff (abs (- q q-int))
        r-diff (abs (- r r-int))]
    (cond
      (and (> p-diff q-diff)
            (> p-diff r-diff))
       (hexagon (* (+ q-int r-int) -1) q-int r-int)
      (and (> q-diff r-diff)
            (> q-diff p-diff))
       (hexagon p-int (* (+ p-int r-int) -1) r-int)
      :else (hexagon p-int q-int (* (+ p-int q-int) -1)))))

(defn facing 
  "Finds which hexside a line starting from the center of the hex and
   reaching a point beyond the hex passes through. Used for changing facing"
  [o destination layout]
  (let [origin (hex-to-pixel o layout) 
        angle (math/to-degrees (math/atan2 (- (:x destination) (:x origin)) (- (:y destination) (:y origin))))]
    (cond
      (<= 150 (abs angle))  :n
      (and (> 150 angle)  (>= angle 90))  :ne 
      (and (> 90 angle)  (>= angle 30)) :se 
      (and (> 30 angle) (>= angle -30)) :s
      (and (> -30 angle) (>= angle -90)) :sw
      (and (> -90 angle) (>= angle -150)) :nw
      :else :n)))

;; Commented out in case I need it later. I believe that cljfx
;; has given me this feature for "free" when I added a click event
;; to the hexagons.
;; (defn pixel-to-hex
;;   [pt layout]
;;   (let [pth (:pixel-to-hex-matrix layout)
;;         modified-point {:x (/ (- (:x pt)
;;                                  (:x-origin layout))
;;                               (:x-size layout))
;;                         :y (/ (- (:y pt)
;;                                  (:y-origin layout))
;;                               (:y-size layout))}
;;         p (+ (* (:x modified-point) (get pth 0))
;;              (* (:y modified-point) (get pth 1)))
;;         q (+ (* (:x modified-point) (get pth 2))
;;              (* (:y modified-point) (get pth 3)))]
;;     (round p q (* (+ p q) -1))))