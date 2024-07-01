(ns megastrike.combat-unit
  (:require [clojure-csv.core :as csv]
            [clojure.math :as math]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.hexagons.hex :as hexagon]
            [megastrike.utils :as utils]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map utils/keyword-maker
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(def all-types ["BM" "IM" "PM" "SV" "CV" "BA" "CI" "SS"
                "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def ground-units ["BM" "IM" "PM" "SV" "CV" "BA" "CI"])
(def aero-units ["SS" "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def bm-units ["BM"])
(def mech-units ["BM" "IM" "PM"])
(def conventional-units ["SV" "CV" "BA" "CI"])
(def vehicle-units ["SV" "CV"])
(def infantry-units ["BA" "CI"])

(def directions {:n  {:angle 0 :ordinal 2}
                 :ne {:angle 60 :ordinal 1}
                 :se {:angle 120 :ordinal 0}
                 :s  {:angle 180 :ordinal 5}
                 :sw {:angle 240 :ordinal 4}
                 :nw {:angle 300 :ordinal 3}})

(defn move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :walk
      (= mv-key (utils/keyword-maker "j")) :jump
      :else (utils/keyword-maker mv-type))))

(defn parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :jump))
      (merge mv-map {:walk (val (first mv-map))})
      mv-map)))

(defn print-movement-helper
  "Consumes a vector containing a move type as a keyword and a distance and prints it for human consumption."
  [mv-vec]
  (cond
    (= (first mv-vec) :walk) (second mv-vec)
    (= (first mv-vec) :jump) (str (second mv-vec) "j")
    :else (str (first mv-vec) " " (second mv-vec))))

(defn print-movement
  "Loops over all movements a unit has a pretty prints them."
  [unit]
  (let [mv-map (:movement unit)]
    (str/join "/" (map print-movement-helper mv-map))))

(defn construct-ability-list
  "Loops over all abilitys a unit has a converts them to Keywords."
  [str]
  (into [] (map utils/keyword-maker (str/split str #","))))

(defn parse-row
  "Parses a single row of the MUL file."
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

(def mul (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn filter-membership-helper
  "Returns true if a unit matches one of the types."
  ([unit]
   unit)
  ([unit field values]
   (some #(= (field unit) %) values)))

(defn filter-units
  "Filters units based on either a string or a seq of unit type."
  ([units]
   units)
  ([units field value comparison]
   (filter #(when (comparison (field %) value) %) units))
  ([units field values]
   (filter #(filter-membership-helper % field values) units)))

(defn get-unit
  ([unit]
   (let [non-standard (str/replace unit #"\(Standard\)" "")
         matching-muls (filter-units mul :full-name unit str/includes?)
         non-standard-mul (filter-units mul :full-name non-standard =)] 
     (if (first matching-muls)
       (first matching-muls)
       (first non-standard-mul)))))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data))
  ([units mul-unit game-data]
   (let [matching-units (filter (fn [x] (when (and (:id x) (:full-name mul-unit)) 
                                   (str/includes? (:id x) (:full-name mul-unit)))) (vals units))
         id (if (seq matching-units)
              (str (:full-name mul-unit) " #" (inc (count matching-units)))
              (str (:full-name mul-unit)))
         unit (merge mul-unit {:id id} game-data)]
     (merge units {id unit}))))

(defn pv-mod
  "Calculates the skill-based mod for PV based on the algorithm provided in the book."
  [unit]
  (let [skill-diff (- 4 (:skill (:pilot unit)))]
    (cond
      (> 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- (:point-value unit) 5) 10)))
      (< 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- (:point-value unit) 3) 5)))
      :else 0)))

(defn pv
  "Returns the modified PV."
  [unit]
  (+ (:point-value unit) (pv-mod unit)))

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
  "Parses a single line from a mechset file. Mechset files define which images match which units."
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
  "Parses a full Mechset file."
  []
  (into [] (remove
            nil?
            (map #(parse-mechset-line %)
                 (str/split-lines (slurp (utils/load-resource :data "images/units/mechset.txt")))))))

(def mechset (parse-mechset))

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [unit]
  (let [chassis-match (filter (fn [row] (= (second row) (:chassis unit))) mechset)
        exact-match (filter (fn [row] (str/includes? (second row) (:full-name unit))) mechset)
        match-row (or (first exact-match) (first chassis-match))] 
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

(defn find-path
  [unit destination board]
  (let [origin (hexagon/find-hex unit (board/nodes board))
        mv-type (get unit :movement-mode :walk)]
    (board/astar origin destination board hexagon/hex-distance mv-type)))

(defn move-costs 
  [unit board]
  (let [origin (hexagon/find-hex unit (board/nodes board))
        mv-type (get unit :movement-mode :walk)]
    (loop [sum [(hexagon/step-cost origin (first (:path unit)) mv-type)]
                   path (:path unit)]
              (if (= (count path) 1) 
                sum 
                (recur (conj sum (hexagon/step-cost (first path) (second path) mv-type)) 
                       (rest path))))))

(defn can-move?
  "Checks whether or not a unit can move from its location to a destination."
  [unit board]
  (cond
    (= (:movement-mode unit) :stand-still) (merge unit {:acted true})
    (seq (:path unit)) (let [sum (reduce + (move-costs unit board))
                             unit (if (not (:movement-mode unit))
                                    (assoc unit :movement-mode (key (first (:movement unit))))
                                    unit)
                             move (get-in unit [:movement (:movement-mode unit)])] 
                         (if (<= sum move)
                           (merge unit 
                                  (select-keys (last (:path unit)) [:p :q :r])
                                  {:acted true :path []})
                           unit))))

(defn calculate-attacker-mod
  "Returns the mod for a given to hit due to the attacker's movement mode this turn."
  [unit]
  (cond
    (= (:movement-mode unit) :immobile) -1
    (= (:movement-mode unit) :standstill) -1
    (= (:movement-mode unit) :jump) 2
    :else 0))

(defn calculate-target-mod
  "Calculates the mod for the to hit to an attack based on the target's condition."
  [unit]
  (cond
    (= (:movement-mode unit) :immobile) -4
    (= (:movement-mode unit) :standstill) 0
    (= (:movement-mode unit) :jump) (+ (:tmm unit) 1)
    :else (:tmm unit)))

(defn calculate-other-mod
  "Calculate 'other' modifiers to the to hit. Terrain, heat, etc."
  [attacker target]
  (:current-heat attacker))

(defn calculate-range-mod
  "Calculates the mod to hit based on the range."
  [attacker target]
  (let [range (hexagon/hex-distance attacker target)]
    (cond
      (>= 3 range) 0
      (>= 12 range) 2
      (>= 21 range) 4
      (>= 30 range) 6
      :else nil)))

(defn calculate-to-hit
  "Calculates the to hit for an attack using the SATOR method from the book."
  [attacker target]
  (+ (:skill (:pilot attacker))
     (calculate-attacker-mod attacker)
     (calculate-target-mod target)
     (calculate-other-mod attacker target)
     (calculate-range-mod attacker target)))

(defn calculate-damage
  "Returns the damage done by a unit at a given range. Calculates 0* damage correctly."
  [unit range]
  (cond
    (>= 3 range) (if (and (:s* unit) (<= 4 (utils/roll-die))) 1 (:s unit))
    (>= 12 range) (if (and (:m* unit) (<= 4 (utils/roll-die))) 1 (:m unit))
    (>= 21 range) (if (and (:l* unit) (<= 4 (utils/roll-die))) 1 (:l unit))
    (>= 30 range) (if (and (:e* unit) (<= 4 (utils/roll-die))) 1 (:e unit))))

(defn take-damage
  "Applies damage 1 point at a time, checking to see if there armor remaining."
  [unit damage]
  (if (= damage 0)
    unit
    (loop [armor (:current-armor unit (:armor unit))
           structure (:current-structure unit (:structure unit))
           damage damage]
      (if (zero? damage)
        (assoc unit :current-armor armor :current-structure structure)
        (let [arm (dec armor)
              str (dec structure)
              dmg (dec damage)]
          (if (pos? arm)
            (recur arm structure dmg)
            (recur 0 str dmg)))))))

(defn make-attack
  "Rolls a full attack. Calculating the to-hit, rolling the dice, and then applying the damage and returning the targeted unit."
  [attacker target]
  (let [target-num (calculate-to-hit attacker target)
        range (hexagon/hex-distance attacker target)
        to-hit (utils/roll2d)]
    (if (<= target-num to-hit)
      (take-damage target (calculate-damage attacker range))
      target)))
