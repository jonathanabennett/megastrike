(ns megastrike.phases
  (:require
   [clojure.math :as math]
   [com.brunobonacci.mulog :as mu]
   [megastrike.combat-unit :as cu]
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
            unit-totals (frequencies (map cu/get-force units))]
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
  (let [forces (roll-initiative forces)
        initiative-report (reduce str (map #(str (:name %) " rolled a " (:initiative %) "\n") (vals forces)))
        turn-num (inc turn-number)
        turn-string (str "Turn: " turn-num)
        move-list (str "Turn Order: " (reduce str (map #(str % ", ") (generate-turn-order forces (vals units)))))
        round-report (str turn-string "\n" initiative-report move-list "\n\n----------\n")]
    (mu/log ::begin-initiative-phase
            :turn-number turn-num
            :initiative-rolls initiative-report
            :turn-order move-list
            :current-phase "Initiative"
            :instrumentation :player)
    {:current-phase :initiative :turn-number turn-num :forces forces :turn-order () :units units :round-report round-report}))

(defn start-deployment-phase
  "Generates the turn order based on the number of units who haven't been deployed yet."
  [{:keys [forces units round-report]}]
  (let [deployable-units (remove (fn [unit] (cu/deployed? unit)) (vals units))
        turn-order (generate-turn-order forces deployable-units)
        round-string (str "Deployment Phase\n" "Deployment order: " (reduce str (map #(str % ", ") turn-order)) "\n\n----------\n")
        report (str round-report round-string)]
    (mu/log ::begin-deployment-phase
            :deployable-units (map :id deployable-units)
            :turn-order turn-order
            :current-phase "Deployment"
            :instrumentation :player)
    {:current-phase :deployment :turn-order turn-order :units units :round-report report}))

(defn start-movement-phase
  "Regenerates the turn order. Nothing else special is required."
  [{:keys [forces units round-report]}]
  (let [turn-order (generate-turn-order forces (vals units))
        round-string (str "Movement Phase \n" "Movement Order: " (reduce str (map #(str % ", ") turn-order)) "\n\n----------\n")
        report (str round-report round-string)]
    (mu/log ::begin-movement-phase
            :turn-order turn-order
            :current-phase "Movement"
            :instrumentation :player)
    {:current-phase :movement :turn-order turn-order :units units :round-report report}))

(defn start-combat-phase
  "Simply regenerate the turn order, but with each force only in the turn order once."
  [{:keys [forces units round-report]}]
  (let [turn-order (generate-turn-order forces)
        round-string (str "Combat Phase \n" "Attack Order: " (reduce str (map #(str % ", ") turn-order)) "\n\n----------\n")
        report (str round-report round-string)]
    (mu/log ::begin-combat-phase
            :turn-order turn-order
            :current-phase "Combat"
            :instrumentation :player)
    {:current-phase :combat :turn-order turn-order :units units :round-report report}))

(defn start-end-phase
  "Remove all targeting as part of the end phase process."
  [{:keys [units]}]
  (let [units (into {} (for [[_ unit] units] (cu/end-turn unit)))]
    (mu/log ::begin-end-phase
            :current-phase "End")
    {:current-phase :end :turn-order nil :units units}))

(defn next-phase
  "Removes destroyed units and resets the acted status on every unit, then dispatches to the correct phase method."
  [{:keys [current-phase turn-number forces units round-report]}]
  (let [new-units (into {} (for [[k unit] units] [k (cu/end-phase unit)]))]
    (mu/with-context {:turn-number turn-number}
      (cond
        (= current-phase :initiative) (start-deployment-phase {:forces forces :units new-units :round-report round-report})
        (= current-phase :deployment) (start-movement-phase {:forces forces :units new-units :round-report round-report})
        (= current-phase :movement)   (start-combat-phase {:forces forces :units new-units :round-report round-report})
        (= current-phase :combat)     (start-end-phase {:units new-units :round-report round-report})
        (= current-phase :end)        (start-initiative-phase {:forces forces :turn-number turn-number :units new-units :round-report round-report})
        :else (start-initiative-phase {:forces forces :turn-number turn-number :units new-units :round-report round-report})))))
