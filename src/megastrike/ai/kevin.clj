(ns megastrike.ai.kevin
  "This is Kevin. He's our basic Computer Player."
  (:require
   [clojure.data.priority-map :as priority-map]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(defn ->kevin
  [unit {:keys [aggression home-edge] :or {aggression 0 home-edge :n}}]
  {:unit (cu/id unit)
   :damage-bar (+ (cu/get-remaining-armor unit) (* (cu/get-remaining-structure unit) 2))
   :target nil
   :aggression aggression
   :home-edge home-edge})

(defn target-info
  [attacker target board layout attack]
  (let [targeting (second (cu/->targeting attacker target board layout attack))
        target-number (cu/calculate-to-hit targeting)
        probability (get utils/probabilities target-number 0)
        toughness (+ (cu/get-remaining-armor target) (* (cu/get-remaining-structure target) 2))
        damage (:damage targeting)
        damage-num (if (str/ends-with? damage "*") 0.5 (Integer/parseInt damage))
        expected-damage (/ (* probability damage-num) 100.0)]
    (mu/log ::targeting-info
            :targeting targeting)
    [(cu/id target)
     {:firing-solution targeting
      :toughness toughness
      :expected-damage expected-damage
      :percentage damage-num}]))

(defn targeting-options
  [unit units board layout]
  (map #(target-info unit % board layout :regular) units))

(defn calculate-defensive-value
  [unit units board layout]
  (let [counter-attacks (map #(target-info % unit board layout :regular) units)
        counter-damage (map #(get (second %) :expected-damage 0) counter-attacks)
        total (/ (reduce + counter-damage) (count counter-attacks))]
    (mu/log ::defensive-value
            :total total)
    total))

(defn calculate-offensive-value
  [unit units board layout]
  (let [attacks (map #(target-info unit % board layout :regular) units)
        expected-damage (map #(get (second %) :expected-damage 0) attacks)
        total (/ (reduce + expected-damage) (count expected-damage))]
    (mu/log ::offensive-value
            :total total)
    total))

(defn create-movement-option
  [path unit units board layout mv-type]
  (let [temp-unit (-> unit
                      (cu/set-path (second path))
                      (cu/set-movement-mode mv-type)
                      (cu/move-unit))
        cost (reduce + (board/path-cost (second path) mv-type units))
        move-option {:destination (first path)
                     :path (second path)
                     :cost (reduce + (board/path-cost (second path) mv-type units))}]
    (if (<= cost (cu/get-mv unit mv-type))
      [move-option (- (calculate-offensive-value temp-unit units board layout) (calculate-defensive-value temp-unit units board layout))]
      [move-option ##-Inf])))

(defn zero-weight
  "Used in Kevin's A* algorithm so that it parses the whole map."
  [_ _]
  0)

(defn move-options
  [unit units board layout]
  (let [mv-type (cu/get-selected-movement unit true)
        unit-loc (board/find-hex (cu/get-location unit) board)
        updated-board (cu/set-stacking board units)
        hostiles (filter #(not= (cu/get-force %) (cu/get-force unit)) units)
        paths (into (priority-map/priority-map-by >)
                    (map #(create-movement-option % unit hostiles updated-board layout mv-type)
                         (movement/astar unit-loc false updated-board zero-weight mv-type (cu/get-force unit))))]
    (mu/log ::verifying-move-options
            :astar (movement/astar unit-loc false updated-board zero-weight mv-type (cu/get-force unit))
            :options (take 5 paths))
    (first (rand-nth (take 5 paths)))))

(defn naive-target-selection
  [options]
  (rand-nth options))

(defn select-target
  [options]
  (:firing-solution (second (naive-target-selection options))))
