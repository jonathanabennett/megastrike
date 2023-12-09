(ns megastrike.combat-unit
  (:require
   [clojure-csv.core :as csv]
   [clojure.math :as math]
   [clojure.string :as str]
   [megastrike.hexagons.hex :as hexagon]
   [megastrike.utils :as utils]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map utils/keyword-maker
       (first (csv/parse-csv (slurp "resources/mul.csv") :delimiter \tab))))

(def all-types ["BM" "IM" "PM" "SV" "CV" "BA" "CI" "SS"
                "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def ground-units ["BM" "IM" "PM" "SV" "CV" "BA" "CI"])
(def aero-units ["SS" "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def bm-units ["BM"])
(def mech-units ["BM" "IM" "PM"])
(def conventional-units ["SV" "CV" "BA" "CI"])
(def vehicle-units ["SV" "CV"])
(def infantry-units ["BA" "CI"])

(defn move-keyword
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :walk
      (= mv-key (utils/keyword-maker "j")) :jump
      :else (utils/keyword-maker mv-type))))

(defn parse-movement
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)]
    (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))))

(defn print-movement-helper
  [mv-vec]
  (cond
    (= (first mv-vec) :walk) (second mv-vec)
    (= (first mv-vec) :jump) (str (second mv-vec)"j")
    :else (str (first mv-vec) " " (second mv-vec))
    ))

(defn print-movement
  [unit]
  (let [mv-map (:movement unit)]
    (str/join "/" (map print-movement-helper mv-map))))

(defn construct-ability-list
  [str]
  (into [] (map utils/keyword-maker (str/split str #","))))

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

(defn filter-membership-helper
  ([unit]
   unit)
  ([unit field values]
   (some #(= (field unit) %) values)))

(defn filter-units
  ([units]
   units)
  ([units field value comparison]
   (filter #(when (comparison (field %) value) %) units))
  ([units field values]
   (filter #(filter-membership-helper % field values) units)))

(defn filter-membership
  ([units]
   units)
  ([units field values]
   (filter #(filter-membership-helper % field values) units)))

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

(defn print-short
  [unit]
  (if (:s* unit)
    "0*"
    (str (:s unit))))

(defn print-medium
  [unit]
  (if (:m* unit)
    "0*"
    (str (:m unit))))

(defn print-long
  [unit]
  (if (:l* unit)
    "0*"
    (str (:l unit))))

(defn print-extreme
  [unit]
  (if (:e* unit)
    "0*"
    (str (:e unit))))

(defn parse-mechset-line
  [line]
  (when-not (or (= (str/index-of line "#") 0)
                (= line "")
                (= (str/index-of line "include") 0))
    (let [first-break (str/index-of line " ")
          second-break (str/index-of line "\" " (inc first-break))
          mechset-type (str/trim (subs line 0 first-break))
          search-term (str/trim (utils/strip-quotes (subs line first-break second-break)))
          file-path (str/trim (utils/strip-quotes (subs line second-break)))]
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
  unit)

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
  [attacker target]
  (+ (:skill (:pilot attacker))
     (calculate-attacker-mod attacker)
     (calculate-target-mod target)
     (calculate-other-mod target)
     (calculate-range-mod attacker target)))

(defn print-damage
  [unit range]
  (cond
    (>= 3 range) (print-short unit)
    (>= 12 range) (print-medium unit)
    (>= 21 range) (print-long unit)
    (>= 30 range) (print-extreme unit)))

(defn calculate-damage
  [unit range]
  (cond
    (>= 3 range) (if (and (:s* unit) (<= 4 (utils/roll-die))) 1 (:s unit))
    (>= 12 range) (if (and (:m* unit) (<= 4 (utils/roll-die))) 1 (:m unit))
    (>= 21 range) (if (and (:l* unit) (<= 4 (utils/roll-die))) 1 (:l unit))
    (>= 30 range) (if (and (:e* unit) (<= 4 (utils/roll-die))) 1 (:e unit))))

(defn take-damage
  [unit damage]
  (if (= damage 0)
    unit
    (if (< 0 (:current-armor unit (:armor unit)))
      (take-damage (assoc unit :current-armor (dec (:current-armor unit (:armor unit)))) (dec damage))
      (take-damage (assoc unit :current-structure (dec (:current-structure unit (:structure unit)))) (dec damage)))))

(defn make-attack
  [attacker target]
  (let [target-num (calculate-to-hit attacker target)
        range (hexagon/hex-distance attacker target)
        to-hit (utils/roll2d)]
    (when (<= target-num to-hit)
      (take-damage target (calculate-damage attacker range)))))
