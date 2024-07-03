(ns megastrike.phases
  (:require [clojure.math :as math]
            [megastrike.utils :as utils]))

(defn roll-initiative 
  "Rolls initiative for the units, repeating the roll until all the forces have unique rolls."
  [forces]
  (let [rolls (zipmap (keys forces) (repeatedly utils/roll2d))]
    (if-not (apply distinct? (vals rolls))
      (recur forces)
      (->> forces
           (map (fn [[f force]]
                  [f (assoc force :initiative (f rolls))]))
           (into {})))))

(defn move-generator
  "Takes a map containing {:force-name count} pairs and returns the number which
   should move this round"
  [unit-count force-list]
  (let [smallest-count (first (sort < (vals unit-count)))] 
    (loop [ret {}
           forces force-list]
      (if (empty? forces)
        ret
        (let [f (utils/keyword-maker (:name (first forces)))] 
          (recur (assoc ret f (max 1 (math/floor-div (f unit-count) smallest-count))) 
                 (rest forces)))))))

(defn generate-turn-order 
  "Given forces with an initiative rolled and units, it generates a turn order which respects the initiative order algorithm from Alpha Strike."
  ([forces units]
   (let [forces (sort-by :initiative (vals forces))] 
     (loop [turn-order []
            unit-totals (frequencies (map :force units))] 
       (if (= (reduce + (vals unit-totals)) 0)
         (flatten turn-order) 
         (let [unit-pairs (move-generator unit-totals forces)]
           (recur (conj turn-order (map (fn [[key value]] (take value (repeat key))) unit-pairs))
                  (into {} (map (fn [[key value]] [key (- value (get unit-pairs key))]) unit-totals))))))))
  ([forces]
   (into [] (map #(utils/keyword-maker (:name %)) (sort-by :initiative (vals forces))))))

(defn start-initiative-phase 
  "Reroll the initiative, increment the turn number, save the new forces (with their initiative), but do not generate a turn order."
  [{:keys [turn-number forces units]}]
  (let [forces (roll-initiative forces)]
    {:current-phase :initiative :turn-number (inc turn-number) :forces forces :turn-order nil :units units}))

(defn start-deployment-phase 
  "Generates the turn order based on the number of units who haven't been deployed yet."
  [{:keys [forces units]}] 
  (let [deployable-units (remove (fn [unit] (number? (:q unit))) (vals units))]
    {:current-phase :deployment :turn-order (generate-turn-order forces deployable-units) :units units}))

(defn start-movement-phase 
  "Regenerates the turn order. Nothing else special is required."
  [{:keys [forces units]}]
  {:current-phase :movement :turn-order (generate-turn-order forces (vals units)) :units units})

(defn start-combat-phase 
  "Simply regenerate the turn order, but with each force only in the turn order once."
  [{:keys [forces units]}]
  {:current-phase :combat :turn-order (generate-turn-order forces) :units units})

(defn start-end-phase 
  "Remove all targeting as part of the end phase process."
  [{:keys [units]}] 
  (let [targeting-removed (into {} (for [[k unit] units] (if (not-any? #(= (:target unit) %) (keys units)) [k (assoc unit :target nil)]
                                                             [k unit])))] 
    {:current-phase :end :turn-order nil :units (into {} (for [[k unit] targeting-removed] [k (assoc unit :movement-mode nil)]))}))

(defn next-phase 
  "Removes destroyed units and resets the acted status on every unit, then dispatches to the correct phase method."
  [{:keys [current-phase turn-number forces units]}]
  (let [remaining (into {} (for [[k unit] units] (when (or (pos? (get unit :current-structure (get unit :structure))) (not (:destroyed? unit))) [k unit])))
        new-units (into {} (for [[k unit] remaining] [k (assoc unit :acted nil)]))]
    (cond 
     (= current-phase :initiative) (start-deployment-phase {:forces forces :units new-units})
     (= current-phase :deployment) (start-movement-phase {:forces forces :units new-units})
     (= current-phase :movement)   (start-combat-phase {:forces forces :units new-units})
     (= current-phase :combat)     (start-end-phase {:units new-units})
     (= current-phase :end)        (start-initiative-phase {:forces forces :turn-number turn-number :units new-units})
     :else (start-initiative-phase {:forces forces :turn-number turn-number :units new-units}))))
