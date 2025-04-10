(ns megastrike.combat-unit
  (:require
   [clojure-csv.core :as csv]
   [clojure.math :as math]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.attacks :as attacks]
   [megastrike.board :as board]
   [megastrike.damage :as damage]
   [megastrike.heat :as heat]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

;; MUL Units
(defn- move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :move/walk
      (= mv-key (utils/keyword-maker "j")) :move/jump
      :else (keyword "move" (-> mv-type
                                (string/trim)
                                (string/lower-case)
                                (utils/remove-parens)
                                (utils/correct-range-brackets)
                                (utils/replace-spaces))))))

(defn parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :move/jump))
      (merge mv-map {:move/walk (val (first mv-map))})
      mv-map)))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map #(keyword (utils/keyword-maker %))
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-row
  "Parses a single row of the MUL file."
  ([row]
   (parse-row header-row row))
  ([hr row]
   (let [mul-row (zipmap hr row)
         modes (parse-movement (:movement mul-row))
         abilities (abilities/parse-abilities (:abilities mul-row))]
     (s/assert :unit/mul
               {:unit/chassis (:chassis mul-row)
                :unit/model (:model mul-row)
                :unit/role (keyword "role" (utils/keyword-maker (:role mul-row)))
                :unit/type (keyword "type" (utils/keyword-maker (:type mul-row)))
                :unit/threshold (Integer/parseInt (:threshold mul-row))
                :unit/full-name (str (:chassis mul-row) " " (:model mul-row))
                :unit/mul-id (Integer/parseInt (:mul-id mul-row))
                :unit/size (Integer/parseInt (:size mul-row))
                :unit/move-modes modes
                :unit/structure {:toughness/current (Integer/parseInt (:armor mul-row))
                                 :toughness/maximum (Integer/parseInt (:armor mul-row))
                                 :toughness/unapplied 0}
                :unit/armor {:toughness/current (Integer/parseInt (:armor mul-row))
                             :toughness/maximum (Integer/parseInt (:armor mul-row))
                             :toughness/unapplied 0}
                :unit/tmm (Integer/parseInt (:tmm mul-row))
                :unit/attacks (attacks/->attacks mul-row modes abilities)
                :unit/overheat (Integer/parseInt (:overheat mul-row))
                :unit/abilities abilities
                :unit/base-pv (Integer/parseInt (:point-value mul-row))}))))

(def mul
  (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-mechset-line
  "Parses a single line from a mechset file. Mechset files define which images match which units."
  [line]
  (when-not (or (= (string/index-of line "#") 0)
                (= line "")
                (= (string/index-of line "include") 0))
    (let [first-break (string/index-of line " ")
          second-break (string/index-of line "\" " (inc first-break))
          mechset-type (string/trim (subs line 0 first-break))
          search-term (string/trim (utils/strip-quotes (subs line first-break second-break)))
          file-path (string/trim (utils/strip-quotes (subs line second-break)))]
      (vector mechset-type search-term file-path))))

;; SPRITE Access
(defn parse-mechset
  "Parses a full Mechset file."
  []
  (into [] (remove
            nil?
            (map #(parse-mechset-line %)
                 (string/split-lines (slurp (utils/load-resource :data "images/units/mechset.txt")))))))

(def mechset (parse-mechset))

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [{:keys [unit/chassis unit/full-name]}]
  (let [chassis-match (filter (fn [row] (= (second row) chassis)) mechset)
        exact-match (filter (fn [row] (string/includes? (second row) full-name)) mechset)
        match-row (or (first exact-match) (first chassis-match))]
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

;; COMBAT Units
(defn ->combat-unit
  ([mul-unit pilot facing location battle-force number]
   (s/assert :unit/combat-unit
             (merge mul-unit
                    {:unit/id (if (pos? number)
                                (str (:unit/full-name mul-unit) " #" (inc number))
                                (:unit/full-name mul-unit))
                     :unit/battle-force battle-force
                     :unit/pilot pilot
                     :unit/acted? false
                     :unit/facing facing
                     :unit/location location
                     :unit/path []
                     :unit/criticals {:crits/taken [] :crits/unapplied []}
                     :move/selected nil
                     :move/default (if (contains? (:unit/move-modes mul-unit) :move/walk) :move/walk (first (keys (:unit/move-modes mul-unit))))
                     :unit/overheat-used 0
                     :unit/current-heat 0
                     :unit/sprite (find-sprite mul-unit)})))
  ([{:keys [units mul-unit pilot battle-force facing location] :or {facing :direction/none location {}}}]
   (->combat-unit mul-unit pilot facing location battle-force (count (filter #(= (:unit/full-name %) (:unit/full-name mul-unit)) units)))))

(defn filter-membership-helper
  "Returns true if a unit matches one of the types."
  ([unit]
   unit)
  ([unit field values]
   (prn (field unit))
   (contains? values (field unit))))

(defn filter-units
  "Filters units based on either a string or a seq of unit type."
  ([units]
   units)
  ([units field value comparison]
   (filter #(when (comparison (field %) value) %) units))
  ([units field values]
   (filter #(filter-membership-helper % field values) units))
  ([units unit-type]
   (prn unit-type)
   (filter #(s/valid? unit-type (:unit/type %)) units)))

(defn get-unit
  ([s]
   (let [non-standard (string/replace s #"\(Standard\)" "")
         matching-muls (filter-units mul :unit/full-name s =)

         non-standard-mul (filter-units mul :unit/full-name non-standard =)]
     (if (first matching-muls)
       (first matching-muls)
       (first non-standard-mul)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUL/Core

(defn pv-mod
  "Calculates the skill-based mod for PV based on the algorithm provided in the book."
  [{:keys [unit/pilot unit/base-pv]}]
  (let [skill-diff (- 4 (:pilot/skill pilot))]
    (cond
      (> 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- base-pv 5) 10)))
      (< 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- base-pv 3) 5)))
      :else 0)))

(defn pv
  "Returns the modified PV."
  [{:keys [unit/base-pv] :as unit}]
  (+ base-pv (pv-mod unit)))

(defn set-stacking
  "Mark all units on the board."
  [board units]
  (let [b (board/set-stacking board (for [u units] [(:unit/location u) (:unit/battle-force u)]))]
    (prn (map :stacking b))
    b))

(defn set-path
  ([unit hex board units]
   (movement/set-path unit hex (set-stacking board (vals units))))
  ([unit path]
   (assoc unit :unit/path path)))

(defn move-unit
  [unit]
  (if (movement/can-move? unit (:unit/path unit))
    (-> unit
        (assoc :unit/acted? true)
        (movement/move-unit))
    unit))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Attacks and Damage

(defn print-damage
  [unit bracket]
  (attacks/print-damage unit :attack/regular bracket))

(defn declare-special-attack
  [unit targeting]
  (-> unit
      (assoc :target (:unit/id (:target targeting)))
      (assoc :atk-type (:attack targeting))
      (move-unit)))

(defn can-charge?
  "You can charge a unit if they have acted, you have moved, and they are adjacent to you."
  [attacker target]
  (and (:unit/acted? target) (pos? (count (:unit/path attacker))) (= (hex/distance (last (:unit/path attacker)) (:unit/location target)) 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of Phase and end of turn functions

(defn end-phase
  [unit]
  (assoc unit :unit/acted? false))

(defn end-turn
  "Updates damage, applies weapons crits, resets acted, and then returns the unit IF they
  are not destroyed."
  [unit]
  (let [new-unit (-> unit
                     (assoc :unit/selected nil)
                     (damage/apply-damage)
                     (heat/end-phase-heat false)
                     (assoc :unit/attacked? false))]
    (when-not (:unit/destroyed? unit)
      {(:unit/id new-unit) new-unit})))
