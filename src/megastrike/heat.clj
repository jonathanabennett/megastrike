(ns megastrike.heat
  "Manages heat for AlphaStrike units. Heat affects to-hit rolls, reducing accuracy, but some abilities depend on heat. 
  `->heat` creates a heat map from a map of the values. While this seems redundant, it checks that values are in the 
  correct ranges.

  `add` takes a heat map and increases it. If only given a map, it increases it by 1. Otherwise it increases it by n.
  
  `current` gets the current heat.
  
  `overheat` gets the overheat value.
  
  `shutdown?` returns true if the unit is shut down from heat.

  `reset-heat` sets the current heat to zero after a round of inaction.
  "
  (:require
   [clojure.core :as c]
   [megastrike.damage :as damage]))

(defn shutdown?
  "If current heat is 4 or greater, shutdown the mech."
  [unit]
  (>= (:unit/current-heat unit) 4))

(defn change-heat
  "Applies delta to heat and then ensures that it is in a valid range from 0-4."
  [heat delta]
  (let [new-heat (+ heat delta)]
    (cond
      (< new-heat 0) 0
      (< 4 new-heat) 4
      :else new-heat)))

(defn end-phase-heat
  "Applies heat effects in correct order. First, if the unit was shut down, automatically 
  reset the heat to zero. If the unit wasn't shut down, increase heat by the amount of 
  overheat used. If they are standing in water. Reduce heat by 1. If they made no attack 
  at all this round, reset their heat to zero. Finally, apply any external heat or engine heat."
  [unit water?]
  (prn unit)
  (if (shutdown? unit)
    (assoc unit :unit/current-heat 0)
    (cond-> unit
      ;; If the unit used overheat, add it
      (pos? (:unit/overheat-used unit)) (update :unit/current-heat change-heat (:unit/overheat-used unit))
      ;; If the unit stood in water, subtract 1
      water? (update :unit/current-heat change-heat -1)
      ;; If the unit didn't attack, reset to 0
      (not (:unit/attacked? unit)) (assoc :unit/current-heat 0)
      ;; If the unit has engine damage, add 1
      (pos? (damage/crit-count unit :crits/engine)) (update :unit/current-heat change-heat 1)
      ;; If the unit has unapplied heat damage, ddd it
      (pos? (get unit :unit/unapplied-heat 0)) (update :unit/current-heat change-heat (:unit/unapplied-heat unit))
      ;; Always reset the unapplied heat to 0
      true (assoc :unit/unapplied-heat 0))))
