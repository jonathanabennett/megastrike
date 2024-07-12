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

(def criticals {2 :ammo
                3 :engine
                4 :fire-control
                6 :weapon
                7 :mv 
                8 :weapon
                10 :fire-control
                11 :engine
                12 :destroyed})

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

(defn get-tmm
  ([unit]
   (let [div (count (filter #(= :mv %) (:crits unit)))]
     (loop [tmm (get unit :tmm)
            n 0]
       (if (= n div)
         tmm
         (recur (let [new-tmm (math/round (/ tmm 2.0))]
                  (if (>= (- tmm new-tmm) 1) new-tmm 0))
                (inc n)))))))

(defn get-mv
  ([unit move-type]
   (let [base-move (move-type (:movement unit))
         div (count (filter #(= :mv %) (:crits unit)))] 
     (loop [mv base-move
            n 0]
       (if (= n div)
         (- mv (get unit :current-heat 0))
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([unit]
  (get-mv unit (get unit :movement :walk))))

(defn print-movement-helper
  "Consumes a vector containing a move type as a keyword and a distance and prints it for human consumption."
  [mv-vec unit]
  (cond
    (= (first mv-vec) :walk) (get-mv unit (first mv-vec))
    (= (first mv-vec) :jump) (str (get-mv unit (first mv-vec)) "j")
    :else (str (first mv-vec) " " (get-mv unit (first mv-vec)))))

(defn print-movement
  "Loops over all movements a unit has a pretty prints them."
  [unit]
  (let [mv-map (:movement unit)]
    (str/join "/" (map #(print-movement-helper % unit) mv-map))))

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
            :abilities (:abilities mul-row)
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
         matching-muls (filter-units mul :full-name unit =)
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
  (let [skill-diff (- 4 (get-in unit [:pilot :skill] 4))]
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
  (let [origin (board/find-hex unit board)
        mv-type (get unit :movement-mode :walk)]
    (board/astar origin destination board hexagon/hex-distance mv-type)))

(defn move-costs 
  [unit board]
  (let [origin (board/find-hex unit board)
        mv-type (get unit :movement-mode :walk)]
    (loop [sum [(board/step-cost origin (first (:path unit)) mv-type)]
                   path (:path unit)]
              (if (= (count path) 1) 
                sum 
                (recur (conj sum (board/step-cost (first path) (second path) mv-type)) 
                       (rest path))))))

(defn can-move?
  "Checks whether or not a unit can move from its location to a destination."
  [unit board]
  (cond
    (= (:movement-mode unit) :stand-still) (merge unit {:acted true :path []})
    (seq (:path unit)) (let [sum (reduce + (move-costs unit board))
                             ;; Add code here to default to walk OR the default movement mode
                             unit (if (not (:movement-mode unit))
                                    (assoc unit :movement-mode :walk)
                                    unit)
                             move (get-mv unit (:movement-mode unit))] 
                         (if (<= sum move)
                           (merge unit 
                                  (select-keys (last (:path unit)) [:p :q :r])
                                  {:acted true :path []})
                           unit))))

(defn height-checker
  [origin target line]
  (let [o-height (+ 2 (:elevation origin))
        t-height (+ 2 (:elevation target))]
    (loop [blocked? false
           current (first line)
           l (rest line)]
      (if (or blocked? (= (count l) 1))
        blocked?
        (recur (cond
                 (= (count line) 2) false
                 (hexagon/same-hex origin current)   (>= (:elevation current) o-height)
                 (hexagon/same-hex target (first l)) (>= (:elevation current) t-height)
                 :else (and (>= (:elevation current) o-height) (>= (:elevation current) t-height)))
               (first l)
               (rest l))))))

(defn calculate-attacker-mod
  "Returns the mod for a given to hit due to the attacker's movement mode this turn."
  [unit]
  (let [move (cond 
               (= (:movement-mode unit) :immobile) -1 
               (= (:movement-mode unit) :stand-still) -1 
               (= (:movement-mode unit) :jump) 2 
               :else 0)
        fc (count (filter #(= % :fire-control) (:crits unit)))]
    {:attacker (if (pos? fc) 
                 {:desc (str "Attacker " (name (:movement-mode unit)) " and " fc " fire control hits") :val (+ move (* fc 2))} 
                 {:desc (str "Attacker " (name (get unit :movement-mode :none))) :val move})}))

(defn calculate-target-mod
  "Calculates the mod for the to hit to an attack based on the target's condition."
  [unit]
  {:target (cond 
             (= (:movement-mode unit) :immobile) {:desc "Target immobile" :val -4} 
             (= (:movement-mode unit) :stand-still) {:desc "Target standing still" :val 0} 
             (= (:movement-mode unit) :jump) {:desc "Target jumping" :val (+ (get-tmm unit) 1)} 
             :else {:desc "Target movement modifier" :val (get-tmm unit)})})

(defn calculate-other-mod
  "Calculate 'other' modifiers to the to hit. Terrain, heat, etc."
  [attacker target board]
  (let [heat (get attacker :current-heat 0)
        line (board/hex-line attacker target board)
        blocked? (height-checker (board/find-hex attacker board) (board/find-hex target board) line)
        woods-count (count (filter #(str/includes? (:terrain %) "woods") (rest line)))]
    {:other (if (and (not blocked?) (<= woods-count 3)) 
              (if (zero? woods-count) 
                {:desc "Heat" :val heat} 
                {:desc "Heat and Woods" :val (inc heat)}) 
              {:desc "No Line of Sight" :val ##Inf})}))

(defn calculate-range-mod
  "Calculates the mod to hit based on the range."
  [attacker target]
  (let [range (hexagon/hex-distance attacker target)]
    {:range (cond 
              (>= 3 range) {:desc "Range 1-3" :val 0} 
              (>= 12 range) {:desc "Range 4-12" :val 2} 
              (>= 21 range) {:desc "Range 13-21" :val 4} 
              (>= 30 range) {:desc "Range 22-30" :val 6} 
              :else {:desc "Out of range" :val ##Inf})}))

(defn calculate-to-hit
  "Calculates the to hit for an attack using the SATOR method from the book."
  [attacker target board]
  (let [skill {:skill {:desc "Pilot skill" :val (get-in attacker [:pilot :skill])}}
        atk (calculate-attacker-mod attacker)
        tgt (calculate-target-mod target)
        other (calculate-other-mod attacker target board)
        range (calculate-range-mod attacker target)]
    ;; Map doesn't work this way. I need to combine them all somehow first
    (merge skill atk tgt other range)))

(defn return-to-hit
  [calculation]
  (reduce + [(get-in calculation [:skill :val] 4)
             (get-in calculation [:attacker :val] 0)
             (get-in calculation [:target :val] 0)
             (get-in calculation [:other :val] 0)
             (get-in calculation [:range :val] 0)]))

(defn write-to-hit
  [calculation]
   (reduce str [(get-in calculation [:skill :desc]) ", " 
                (get-in calculation [:attacker :desc])  ", "
                (get-in calculation [:target :desc])  ", "
                (get-in calculation [:other :desc])  ", "
                (get-in calculation [:range :desc])]))

(defn print-damage
  [unit range physical]
  (cond 
     (and (= range 1) physical) (+ (:size unit) (if (str/includes? (:abilities unit) "MEL") 1 0))
     (>= 3 range) (print-short unit)
     (>= 12 range) (print-medium unit)
     (>= 21 range) (print-long unit)
     (>= 30 range) (print-extreme unit)
     :else 0))

(defn calculate-damage
  "Returns the damage done by a unit at a given range. Calculates 0* damage correctly."
  [unit range]
   (cond 
     (and (= range 1) (= (:attack unit) :physical)) (+ (:size unit) (if (str/includes? (:abilities unit) "MEL") 1 0))
     (>= 3 range) (if (and (:s* unit) (<= 4 (utils/roll-die))) 1 (:s unit))
     (>= 12 range) (if (and (:m* unit) (<= 4 (utils/roll-die))) 1 (:m unit))
     (>= 21 range) (if (and (:l* unit) (<= 4 (utils/roll-die))) 1 (:l unit))
     (>= 30 range) (if (and (:e* unit) (<= 4 (utils/roll-die))) 1 (:e unit))
     :else 0))

(defn take-weapon-hit 
  [unit]
  (assoc unit 
         :s (max (dec (:s unit)) 0) 
         :s* false
         :m (max (dec (:m unit)) 0)
         :m* false
         :l (max (dec (:l unit)) 0)
         :l* false
         :e (max (dec (:e unit)) 0)
         :e* false
         :crits (conj (:crits unit) :weapon)))

(defn take-damage
  ([unit damage]
   (take-damage unit damage false))
  ([unit damage tac]
   (if (= damage 0)
     unit
     (let [armor (max (- (:current-armor unit (:armor unit)) damage) 0) 
           penetration (- damage (:current-armor unit (:armor unit)))
           structure (if (zero? armor) 
                       (- (:current-structure unit (:structure unit)) penetration)
                       (:current-structure unit (:structure unit)))
           crit (if (or tac (pos? penetration)) (get criticals (utils/roll2d) nil) nil)
           upd (assoc unit :current-armor armor :current-structure structure)]
       (prn (str damage " damage done to " (:current-armor unit) " armor."))
       (when (zero? armor)
         (prn (str penetration " damage penetrated.")))
       (when (or tac (pos? penetration))
         (prn (str "Rolled " crit " on critical hit table."))) 
       (cond 
         (not (pos? (:current-structure upd (:structure upd)))) (assoc upd :destroyed? true)
         (= crit :ammo) (let [case (str/includes? (:abilities upd) "CASE") 
                              case2 (str/includes? (:abilities upd) "CASEII") 
                              ene (str/includes? (:abilities upd) "ENE")] 
                          (cond (or case2 ene) upd 
                                case (take-damage upd 1) 
                                :else (assoc upd :destroyed? true :crits (conj (:crits upd) :ammo))))
         (= crit :engine) (if (some #(= % :engine) (:crits upd)) 
                            (assoc upd :destroyed? true)
                            (assoc upd :crits (conj (:crits upd) :engine)))
         (= crit :fire-control) (if (< (count (filter #( % :fire-control) (:crits upd))) 4)
                                  (assoc upd :crits (conj (:crits upd) :fire-control))
                                  upd)
         (= crit :weapon) (if (< (count (filter #( % :weapon) (:crits upd))) 4)
                            (take-weapon-hit upd)
                            upd)
         (= crit :mv) (if (< (count (filter #( % :mv) (:crits upd))) 4)
                        (assoc upd :crits (conj (:crits upd) :mv))
                        (assoc upd :movement {:immobile 0}))
         (= crit :destroyed) (assoc upd :destroyed? true :crits (conj (:crits upd) :destroyed))
         :else upd)))))

(defn make-attack
  "Rolls a full attack. Calculating the to-hit, rolling the dice, and then applying the damage and returning the targeted unit."
  [attacker target board]
  (let [targeting-data (calculate-to-hit attacker target board)
        range (hexagon/hex-distance attacker target)
        to-hit (utils/roll2d)] 
    (prn (str (:full-name attacker) " attacking " (:full-name target)))
    (prn (str "To hit: " (return-to-hit targeting-data) ", Rolled: " to-hit))
    (if (<= (return-to-hit targeting-data) to-hit)
      (take-damage target (calculate-damage attacker range) (= to-hit 12))
      target)))
