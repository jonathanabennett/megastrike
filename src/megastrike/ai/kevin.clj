(ns megastrike.ai.kevin
  "This is Kevin. He's our basic Computer Player."
  (:require
   [clojure.data.priority-map :as priority-map]
   [com.brunobonacci.mulog :as mu]
   [megastrike.attacks :as attacks]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.damage :as damage]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(defn target-info
  [attacker target board layout attack]
  (let [targeting (attack (attacks/->targeting attacker target board layout attack))
        target-number (attacks/calculate-to-hit targeting)
        probability (get utils/probabilities target-number 0)
        toughness (damage/health target)
        expected-damage (/ (* (Integer/parseInt (:targeting/damage targeting)) probability) 100)
        percentage (/ expected-damage toughness)]
    [(:unit/acted? target)
     {:firing-solution targeting
      :toughness toughness
      :expected-damage expected-damage
      :percentage percentage
      :probability probability}]))

(defn targeting-options
  [unit units board layout]
  (map #(target-info unit % board layout :regular) units))

(defn calculate-defensive-value
  [unit units board layout]
  (let [counter-attacks (map #(target-info % unit board layout :attack/regular) units)
        counter-damage (map #(get (second %) :expected-damage 0) counter-attacks)
        total (/ (reduce + counter-damage) (count counter-attacks))]
    total))

(defn calculate-offensive-value
  [unit units board layout]
  (let [attacks (map #(target-info unit % board layout :attack/regular) units)
        expected-damage (map #(get (second %) :expected-damage 0) attacks)
        total (/ (reduce + expected-damage) (count expected-damage))]
    total))

(defn max-dealable-damage
  [targets]
  ;; (mu/log ::targeting?
  ;;         :targets targets)
  (->> targets
       (map second)
       (apply max-key :percentage)))

(defn unit-state-score
  [unit hostiles board layout]
  (let [toughness (damage/health-percentage unit)
        targets (targeting-options unit hostiles board layout)
        best-target (max-dealable-damage targets)]
    (mu/log ::unit-state-score
            :best-target best-target)
    (cond
      (> 0.30 toughness) (* (calculate-defensive-value unit hostiles board layout) -1)
      (< 0.70 (:percentage best-target)) (do (mu/log ::unit-state-aggro)
                                             (* (:percentage best-target) 2))
      (pos? (:percentage best-target)) (do (mu/log ::unit-state-can-attack
                                                   :value (:percentage best-target))
                                           (:percentage best-target))
      :else (do (mu/log ::unit-state-default
                        :value (* (hex/distance (:unit/location unit) (:unit/location (first hostiles))) -1))
                (* (hex/distance (:unit/location unit) (:unit/location (first hostiles))) -1)))))

(defn create-movement-option
  [path unit units board layout mv-type]
  (let [temp-unit (-> unit
                      (cu/set-path (second path))
                      (assoc :unit/selected mv-type)
                      (cu/move-unit))
        cost (reduce + (board/path-cost (second path) mv-type units))
        move-option {:destination (first path)
                     :path (second path)
                     :cost (reduce + (board/path-cost (second path) mv-type units))}]
    (if (<= cost (movement/available-mv unit mv-type))
      [move-option (unit-state-score temp-unit units board layout)]
      [move-option ##-Inf])))

(defn zero-weight
  "Used in Kevin's A* algorithm so that it parses the whole map."
  [_ _]
  0)

(defn move-options
  [unit units board layout]
  (let [mv-type (movement/selected-or-default unit)
        unit-loc (board/find-hex (:unit/location unit) board)
        updated-board (cu/set-stacking board units)
        hostiles (filter #(not= (:unit/battle-force %) (:unit/battle-force unit)) units)
        paths (into (priority-map/priority-map-by >)
                    (->> (movement/astar unit-loc false updated-board zero-weight mv-type (:unit/battle-force unit))
                         (map #(create-movement-option % unit hostiles updated-board layout mv-type))
                         (filter #(not (infinite? (second %))))))]
    (mu/log ::top-5-choices
            :options (take 5 paths))
    (first (rand-nth (take 5 paths)))))

(defn naive-target-selection
  [options]
  (rand-nth options))

(defn deadly-target-selection
  [options]
  (max-dealable-damage options))

(defn select-target
  [options]
  (:firing-solution (deadly-target-selection options)))
