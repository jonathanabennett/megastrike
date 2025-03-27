(ns megastrike.heat
  "Manages heat for AlphaStrike units. Heat affects to-hit rolls, reducing accuracy, but some abilities depend on heat. 
  `->heat` creates a heat map from a map of the values. While this seems redundant, it checks that values are in the 
  correct ranges.

  `add` takes a heat map and increases it. If only given a map, it increases it by 1. Otherwise it increases it by n.
  
  `current` gets the current heat.
  
  `overheat` gets the overheat value.
  
  `shutdown?` returns true if the unit is shut down from heat.

  `reset-heat` sets the current heat to zero after a round of inaction.
  ")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas

(defn within-heat-range?
  "Heat must be between 0 and 4."
  [n]
  (<= 0 n 4))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Manipulating heat

(defn add
  "Returns a heat data object with up to `n` heat added.
  Checks to ensure that it will not exceed 4 heat when adding."
  ([{:keys [current] :as heat} n]
   (let [h (+ current n)
         new-heat (assoc heat :current h)]
     (if (within-heat-range? h)
       new-heat
       (add heat (dec n)))))
  ([heat]
   (add heat 1)))

(defn set-heat
  [heat n]
  (assoc heat :current n))

(defn reset-heat
  "Resetting heat sets the current heat to zero."
  [heat]
  (assoc heat :current 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Querying heat and its effects

(defn shutdown?
  "If current heat is 4 or greater, shutdown the mech."
  [heat]
  (<= 4 (:current heat)))

(defn end-phase-heat
  "Applies heat effects in correct order. First, if the unit was shut down, automatically 
  reset the heat to zero. If the unit wasn't shut down, increase heat by the amount of 
  overheat used. If they are standing in water. Reduce heat by 1. If they made no attack 
  at all this round, reset their heat to zero. Finally, apply any external heat or engine heat."
  [heat overheat-used water? no-attack? external-heat]
  (if (or no-attack? (shutdown? heat))
    (reset-heat heat)
    (set-heat heat (cond-> (:heat/current heat)
                     (pos? overheat-used) (+ overheat-used)
                     water? (dec)
                     (pos? external-heat) (+ external-heat)))))
