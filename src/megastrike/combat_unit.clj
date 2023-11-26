(ns megastrike.combat-unit
  (:require [clojure-csv.core :as csv]
            [megastrike.utils :refer [keyword-maker strip-quotes]]
            [megastrike.hexagons.hex :as hexagon]
            [clojure.math :as math]
            [clojure.string :as str]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map keyword-maker
       (first (csv/parse-csv (slurp "resources/mul.csv") :delimiter \tab))))

(defn move-keyword
  [mv-type]
  (let [mv-key (keyword-maker mv-type)]
    (cond
      (= mv-key (keyword-maker "")) :walk
      (= mv-key (keyword-maker "j")) :jump
      :else (keyword-maker mv-type))))

(defn parse-movement
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)]
    (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))))

(defn construct-ability-list
  [str]
  (into [] (map keyword-maker (str/split str #","))))

(defn parse-row
  ([row]
  (parse-row header-row row))
  ([hr row]
  (let [mul-row (zipmap hr row)]
    (assoc mul-row
           :full-name (str (:chassis mul-row) " " (:model mul-row))
           :mul-id (Integer/parseInt (:mul-id mul-row))
           :movement (parse-movement (:movement mul-row))
           :size (Integer/parseInt (:size mul-row))
           :tmm (Integer/parseInt (:tmm mul-row))
           :armor (Integer/parseInt (:armor mul-row))
           :structure (Integer/parseInt (:structure mul-row))
           :threshold (Integer/parseInt (:threshold mul-row))
           :s (Integer/parseInt (:s mul-row))
           :s* (if (= "TRUE" (:s* mul-row)) true false)
           :m (Integer/parseInt (:m mul-row))
           :m* (if (= "TRUE" (:m* mul-row)) true false)
           :l (Integer/parseInt (:l mul-row))
           :l* (if (= "TRUE" (:l* mul-row)) true false)
           :e (Integer/parseInt (:e mul-row))
           :e* (if (= "TRUE" (:e* mul-row)) true false)
           :overheat (Integer/parseInt (:overheat mul-row))
           :point-value (Integer/parseInt (:point-value mul-row))))))

(def mul (map parse-row (rest (csv/parse-csv (slurp "resources/mul.csv") :delimiter \tab))))

(defn filter-units
  ([units]
   units)
  ([units field value comparison]
   (filter #(if (comparison (field %) value) %) units)))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data)))

(defn get-unit
  [li]
  (first li))

(defn pv-mod
  [unit]
  (let [skill-diff (- 4 (:skill (:pilot unit)))]
    (cond
      (> 0 skill-diff) (assoc unit :pv-mod (* skill-diff (+ 1 (math/floor-div (- (:point-value unit) 5) 10))))
      (< 0 skill-diff) (assoc unit :pv-mod (* skill-diff (+ 1 (math/floor-div (- (:point-value unit) 3) 5))))
      :else (assoc unit :pv-mod 0))))

(defn pv
  [unit]
  (+ (:point-value unit) (:pv-mod unit)))

(defn parse-mechset-line
  [line]
  (when-not (or (= (str/index-of line "#") 0)
                (= line "")
                (= (str/index-of line "include") 0))
    (let [first-break (str/index-of line " ")
          second-break (str/index-of line "\" " (inc first-break))
          mechset-type (str/trim (subs line 0 first-break))
          search-term (str/trim (strip-quotes (subs line first-break second-break)))
          file-path (str/trim (strip-quotes (subs line second-break)))]
      (vector mechset-type search-term file-path))))

(defn parse-mechset
  []
  (into [] (remove nil? (map #(parse-mechset-line %) (str/split-lines (slurp "resources/images/units/mechset.txt"))))))

(def mechset (parse-mechset))

(defn find-sprite
  [unit]
  (let [chassis-match (filter (fn [row] (= (:chassis unit) (second row))) mechset)
        exact-match (filter (fn [row] (= (:full-name unit) (second row))) mechset)
        match-row (or (first exact-match) (first chassis-match))]
    (str "resources/images/units/" (nth match-row 2))))

(defn update-position
  [unit destination]
  (merge unit destination))

(defn calculate-attacker-mod
  [unit]
  (cond
    (= (:movement-mode unit) :immobile) -1
    (= (:movement-mode unit) :standstill) -1
    (= (:movement-mode unit) :jump) 2
    :else 0))

(defn calculate-target-mod
  [unit]
  (cond
    (= (:movement-mode unit) :immobile) -4
    (= (:movement-mode unit) :standstill) 0
    (= (:movement-mode unit) :jump) (+ (:tmm unit) 1)
    :else (:tmm unit)))

(defn calculate-other-mod
  [unit]
  0)

(defn calculate-range-mod
  [attacker target]
  (let [range (hexagon/hex-distance attacker target)]
    (cond
      (>= 3 range) 0
      (>= 12 range) 2
      (>= 21 range) 4
      (>= 30 range) 6
      :else nil)))

(defn calculate-to-hit
  []
  0)

(defn print-damage
  []
  0)

(defn calculate-damage
  []
  0)

(defn take-damage
  []
  0)

(defn make-attack
  []
  0)
