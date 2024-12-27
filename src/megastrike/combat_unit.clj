(ns megastrike.combat-unit
  (:require
   [clojure.math :as math]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.attack :as attack]
   [megastrike.combat :as combat]
   [megastrike.heat :as heat]
   [megastrike.mul :as mul]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.pilot :as pilot]
   [megastrike.utils :as utils]))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data {:changes {}}))
  ([units mul-unit game-data]
   (let [matching-units (filter (fn [x] (when (and (:id x) (:full-name mul-unit))
                                          (str/includes? (:id x) (:full-name mul-unit)))) (vals units))
         id (if (seq matching-units)
              (str (:full-name mul-unit) " #" (inc (count matching-units)))
              (str (:full-name mul-unit)))
         unit (merge mul-unit {:id id} game-data {:changes {}})]

     (mu/log ::element-created
             :element unit)
     (merge units {id unit}))))

(defn pv-mod
  "Calculates the skill-based mod for PV based on the algorithm provided in the book."
  [{:keys [pilot point-value]}]
  (let [skill-diff (- 4 (pilot/skill pilot))]
    (cond
      (> 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- point-value 5) 10)))
      (< 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- point-value 3) 5)))
      :else 0)))

(defn pv
  "Returns the modified PV."
  [{:keys [point-value] :as unit}]
  (+ point-value (pv-mod unit)))

(defn calc-charge-damage
  [size tmm]
  (Math/floor (+ size (double (/ tmm 2)))))

(defn calc-dfa-damage
  [size tmm]
  (inc (calc-charge-damage size tmm)))

(defn calc-self-damage
  [{:keys [attack] :as unit} {:keys [size]}]
  (if (= (get attack :flag) :charge)
    (+ (Math/floor (/ (get-tmm unit) 2)) (if (>= size 3) 1 0))
    (:size unit)))

(defn calculate-damage-helper
  [{:keys [s s* m m* l l* e e*]} range rear-attack?]
  (let [damage (cond
                 (>= 3 range) (if (and s* (<= 4 (utils/roll-die))) 1 s)
                 (>= 12 range) (if (and m* (<= 4 (utils/roll-die))) 1 m)
                 (>= 21 range) (if (and l* (<= 4 (utils/roll-die))) 1 l)
                 (>= 30 range) (if (and e* (<= 4 (utils/roll-die))) 1 e)
                 :else 0)]
    (if rear-attack?
      (inc damage)
      damage)))

(defn calculate-damage
  "Returns the damage done by a unit at a given range. Calculates 0* damage correctly."
  [{:keys [attack size abilities] :as unit} range rear-attack?]
  (let [damage (cond
                 (and (= range 1) (= (:flag attack) :physical)) (calc-physical-damage size (contains? abilities :mel))
                 (and (= range 1) (= (:flag attack) :charge)) (calc-charge-damage size (get-tmm unit))
                 (and (= range 1) (= (:flag attack) :dfa)) (calc-dfa-damage size (get-tmm unit))
                 (not= :regular (:flag attack)) (calculate-damage-helper ((:flag attack) abilities) range rear-attack?)
                 :else (calculate-damage-helper false range rear-attack?))]
    (if rear-attack?
      (inc damage)
      damage)))

(defn can-charge?
  "You can charge a unit if they have acted, you have moved, and they are adjacent to you."
  [unit target]
  (and (:acted target) (pos? (count (:path unit))) (= (hex/distance (last (:path unit)) target) 1)))
