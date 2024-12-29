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
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas

(defn within-heat-range?
  "Heat must be between 0 and 4."
  [n]
  (<= 0 n 4))

(def HeatSchema
  [:map [:current number?] [:overheat number?]])

(defn ->heat
  [{:keys [current overheat] :or {current 0 overheat 0}}]
  (let [heat {:current (min current 4)
              :overheat overheat}]
    (when (m/validate HeatSchema heat)
      heat)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Manipulating heat

(defn add
  ([{:keys [current] :as heat} n]
   (let [h (+ current n)
         new-heat (assoc heat :current h)]
     (if (m/validate HeatSchema new-heat)
       new-heat
       (add heat (dec n)))))
  ([heat]
   (add heat 1)))

(defn reset-heat
  [heat]
  (assoc heat :current 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Querying heat and its effects

(defn current
  [heat]
  (get heat :current 0))

(defn overheat
  [heat]
  (get heat :overheat 0))

(defn shutdown?
  [heat]
  (= 4 (:current heat)))

(defn end-phase-heat
  [heat overheat-used water? no-attack? external-heat]
  (if (shutdown? heat)
    (reset-heat heat)
    (cond-> heat
      (pos? overheat-used) (add overheat-used)
      no-attack? (reset-heat)
      (pos? external-heat) (add external-heat))))

(defn heat-effects
  [{:keys [current]}]
  [{:desc "Attacker heat" :value current}])
