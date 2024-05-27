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

(defn generate-turn-order 
  "Given forces with an initiative rolled and units, it generates a turn order which respects the initiative order algorithm from Alpha Strike."
  ([forces units]
   (let [forces (sort-by :initiative (vals forces))]
     (loop [turn-order []
            unit-counts (frequencies (map :force units))
            current-force 0]
       (if (= (reduce + (vals unit-counts)) 0)
         (flatten turn-order)
         (let [fname (utils/keyword-maker (:name (nth forces current-force)))
               num (if (= (first (sort > (vals unit-counts))) 0)
                     (fname unit-counts)
                     (math/floor-div
                      (fname unit-counts)
                      (first (sort > (vals unit-counts)))))]
           (recur (conj turn-order (take num (repeat fname)))
                  (assoc unit-counts fname (- (fname unit-counts) num))
                  (mod (inc current-force) (count forces))))))))
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
  (let [deployable-units (filter #(number? (:q %)) units)
        turn-order (generate-turn-order forces deployable-units)] 
    {:current-phase :deployment :turn-order turn-order :units units}))

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
    {:current-phase :end :turn-order nil :units targeting-removed}))

(defn next-phase 
  "Removes destroyed units and resets the acted status on every unit, then dispatches to the correct phase method."
  [{:keys [current-phase turn-number forces units]}]
  (let [remaining (into {} (for [[k unit] units] (when (pos? (get unit :current-structure (get unit :structure))) [k unit])))
        new-units (into {} (for [[k unit] remaining] [k (assoc unit :acted nil)]))]
    (cond 
     (= current-phase :initiative) (start-deployment-phase {:forces forces :units new-units})
     (= current-phase :deployment) (start-movement-phase {:forces forces :units new-units})
     (= current-phase :movement)   (start-combat-phase {:forces forces :units new-units})
     (= current-phase :combat)     (start-end-phase {:units new-units})
     (= current-phase :end)        (start-initiative-phase {:forces forces :turn-number turn-number :units new-units})
     :else {})))
