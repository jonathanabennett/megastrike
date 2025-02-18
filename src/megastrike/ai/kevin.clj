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
        expected-damage (/ (* probability damage-num) 100)
        percentage (/ expected-damage toughness)]
    ; (mu/log ::target-info
    ;         :damage damage)
    [(cu/id target)
     {:firing-solution targeting
      :toughness toughness
      :percentage percentage}]))

(defn targeting-options
  [unit units board layout]
  (map #(target-info unit % board layout :regular) units))

(defn calculate-defensive-value
  [unit units board layout]
  (let [counter-attacks (map #(target-info % unit board layout :regular) units)
        counter-damage (map #(get (second %) :percentage 0) counter-attacks)]
    ; (mu/log ::defensive-value
    ;         :counter-damage counter-damage)
    (/ (reduce + counter-damage) (count counter-attacks))))

; (defn calculate-offensive-value
;   [unit units board layout]
;   (let [attacks (targeting-options unit units board layout)
;         percentage (map #(get (second %) :percentage 0) attacks)
;         value (/ (reduce + percentage) (count units))]
;     value))

(defn create-movement-option
  [path unit units board layout mv-type]
  (let [temp-unit (-> unit
                      (cu/set-path (second path))
                      (cu/set-movement-mode mv-type)
                      (cu/move-unit))
        cost (board/path-cost (second path) mv-type units)
        defensive-mod (calculate-defensive-value temp-unit units board layout)
        ; attack-mod (calculate-offensive-value temp-unit units board layout)
        move-option {:destination (first path)
                     :path (second path)
                     :cost (board/path-cost (second path) mv-type units)}]

    (if (<= cost (cu/get-mv unit mv-type))
      [move-option defensive-mod]
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
        paths (into (priority-map/priority-map-by >)
                    (map #(create-movement-option % unit units updated-board layout mv-type)
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
