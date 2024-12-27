(ns megastrike.combat-unit
  (:require
   [clojure.math :as math]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.attacks :as attacks]
   [megastrike.board :as board]
   [megastrike.damage :as damage]
   [megastrike.heat :as heat]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.mul :as mul]
   [megastrike.pilot :as pilot]
   [megastrike.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definitions

(def probabilities
  "maps to-hit numbers to hit precentages for display."
  {2  100, 3  98, 4  92, 5  83, 6  72, 7  58, 8  42, 9  28, 10 17, 11 8, 12 3})

(def ExampleElement
  {:id "Stinger STG-3G #2"
   :full-name "Stinger STG-3G"
   :chassis "Stinger"
   :model "STG-3G"
   :heat :a-heat-map
   :movement :a-movement-map
   :attacks :an-attacks-map
   :damage :a-damage-map
   :pilot :a-pilot-map
   :force :a-force-keyword
   :mul-id 823
   :point-value 18})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUL/Core

(defn full-name
  [unit]
  (:full-name unit))

(defn take-action
  [unit]
  (assoc unit :acted true))

(defn id
  [unit]
  (:id unit))

(defn acted?
  [unit]
  (get unit :acted false))

(defn get-force
  [unit]
  (get unit :force))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HEAT

(defn get-heat
  [{:keys [heat]}]
  (heat/current heat))

(defn high-heat?
  [unit]
  (>= (get-heat unit) 2))

(defn overheat
  [{:keys [heat]}]
  (heat/overheat heat))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PILOT

(defn display-pilot
  [unit]
  (pilot/display (:pilot unit)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MOVEMENT

(defn change-movement
  [unit new-movement]
  (assoc unit :movement new-movement))

(defn print-movement
  [{:keys [movement heat]}]
  (movement/print-movement movement (heat/current heat)))

(defn tmm
  [{:keys [movement heat] :as unit}]
  (movement/print-tmm movement (high-heat? unit)))

(defn undeploy
  [{:keys [movement] :as unit}]
  (let [new-movement (movement/set-hex movement {})]
    (change-movement unit new-movement)))

(defn deployed?
  [{:keys [movement]}]
  (movement/deployed? movement))

(defn set-location
  [{:keys [movement] :as unit} hex]
  (let [new-movement (movement/set-hex movement hex)]
    (change-movement unit new-movement)))

(defn set-facing
  [{:keys [movement] :as unit} facing]
  (let [new-movement (movement/set-facing movement facing)]
    (change-movement unit new-movement)))

(defn get-location
  [{:keys [movement]}]
  (movement/get-location movement))

(defn get-path
  [{:keys [movement]}]
  (movement/get-path movement))

(defn get-direction
  [{:keys [movement]}]
  (movement/get-direction movement))

(defn set-path
  [{:keys [movement] :as unit} hex board]
  (change-movement unit (movement/set-path movement hex board)))

(defn set-movement-mode
  [{:keys [movement] :as unit} mode]
  (let [new-movement (movement/set-mode movement mode)]
    (change-movement unit new-movement)))

(defn cancel-movement
  [{:keys [movement] :as unit}]
  (let [new-movement (movement/cancel-movement movement)]
    (change-movement unit new-movement)))

(defn move-unit
  [{:keys [movement heat] :as unit} board]
  (let [current (heat/current heat)]
    (if (movement/can-move? movement current board)
      (-> unit
          (take-action)
          (change-movement (movement/move-unit movement current board)))
      unit)))

(defn get-movement-modes
  [{:keys [movement]}]
  (movement/get-modes movement))

(defn get-movement
  [{:keys [movement]} no-default?]
  (movement/selected movement no-default?))

(defn get-movement-costs
  [{:keys [movement]} board]
  (movement/move-costs movement board))

(defn take-mv-hits
  [{:keys [movement] :as unit} hits]
  (loop [unit unit
         n 0]
    (if (= n hits)
      unit
      (recur (change-movement unit (movement/take-hit movement))
             (inc n)))))

(defn clear-movement
  [{:keys [movement] :as unit}]
  (let [new-movement (movement/clear-movement movement)]
    (change-movement unit new-movement)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ATTACK ACCESS

(defn get-size
  [{:keys [attacks]}]
  (attacks/get-size attacks))

(defn get-remaining-structure
  [{:keys [damage]}]
  (damage/get-remaining-structure damage))

(defn get-remaining-armor
  [{:keys [damage]}]
  (damage/get-remaining-armor damage))

(defn get-current
  [{:keys [damage]} kind]
  (damage/get-current damage kind))

(defn get-max
  [{:keys [damage]} kind]
  (damage/get-max damage kind))

(defn get-crits
  [{:keys [damage]}]
  (apply str (damage/get-crits damage)))

(defn get-new-crits
  [{:keys [damage]}]
  (damage/get-new-crits damage))

(defn print-damage
  [{:keys [attacks]} bracket]
  (attacks/print-damage-bracket (attacks/get-attack attacks :regular) bracket))

(defn take-weapon-hits
  [{:keys [attacks] :as unit} hits]
  (loop [unit unit
         n 0]
    (if (= n hits)
      unit
      (recur (assoc unit :attacks (attacks/take-weaps-hit attacks))
             (inc n)))))

(defn take-fc-hits
  [{:keys [attacks] :as unit} hits]
  (loop [unit unit
         n 0]
    (if (= n hits)
      unit
      (recur (assoc unit :attacks (attacks/take-fc-hit attacks))
             (inc n)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ABILITIES

(defn print-abilities
  [unit]
  (mul/print-abilities unit))

(defn ->element
  ([units mul-unit pilot facing location force]
   (let [matching-units (filter (fn [x] (when (and (:id x) (:full-name mul-unit))
                                          (str/includes? (:id x) (:full-name mul-unit)))) (vals units))
         id (if (seq matching-units)
              (str (:full-name mul-unit) " #" (inc (count matching-units)))
              (str (:full-name mul-unit)))
         unit (-> mul-unit
                  (assoc :id id)
                  (assoc :force force)
                  (assoc :attacked? false)
                  (assoc :pilot (pilot/->pilot pilot))
                  (set-facing facing)
                  (set-location location))]
     (mu/log ::element-created
             :element unit)
     (merge units {id unit})))
  ([units mul-unit pilot force]
   (let [matching-units (filter (fn [x] (when (and (:id x) (:full-name mul-unit))
                                          (str/includes? (:id x) (:full-name mul-unit)))) (vals units))
         id (if (seq matching-units)
              (str (:full-name mul-unit) " #" (inc (count matching-units)))
              (str (:full-name mul-unit)))
         unit (-> mul-unit
                  (assoc :id id)
                  (assoc :force force)
                  (assoc :pilot (pilot/->pilot pilot)))]
     (mu/log ::element-created
             :element unit)
     (merge units {id unit}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MAKE ATTACKS

(defn attacked?
  [unit]
  (:attacked? unit))

(defn clear-attacked
  [unit]
  (assoc unit :attacked false))

(defn woods-mod
  [line]
  (let [woods (count (filter #(str/includes? (:terrain %) "woods") (rest line)))]
    (cond
      (>= woods 3) [{:desc "Line of Sight blocked by woods" :value ##Inf}]
      (str/includes? (:terrain (last line)) "woods") [{:desc "target in woods" :value 1}]
      (pos? woods) [{:desc "intervening woods" :value 1}]
      :else [{:desc "no intervening woods" :value 0}])))

(defn calculate-range-mod
  [range]
  (let [range-str (str "target " range " hexes away")]
    (condp >= range
      3  [{:desc range-str :value 0}]
      12 [{:desc range-str :value 2}]
      21 [{:desc range-str :value 4}]
      30 [{:desc range-str :value 6}]
      [{:desc "Target out of range" :value ##Inf}])))

(defn height-checker
  [origin target line]
  (let [o-height (+ 2 (:elevation (first line)))
        t-height (+ 2 (:elevation (last line)))]
    (if (= (count line) 2)
      false
      (loop [blocked? false
             current (first line)
             l (rest line)]
        (if (or blocked? (= (count l) 1))
          blocked?
          (recur (cond
                   (hex/same-hex origin current)   (>= (:elevation current) o-height)
                   (hex/same-hex target (first l)) (>= (:elevation current) t-height)
                   :else (and (>= (:elevation current) o-height) (>= (:elevation current) t-height)))
                 (first l)
                 (rest l)))))))

(defn vector-subtract [v1 v2]
  (mapv - v1 v2))

(defn cross-product-2d [v1 v2]
  (- (* (first v1) (second v2)) (* (second v1) (first v2))))

(defn line-between-points? [p1 p2 cp op]
  (let [v-line (vector-subtract p2 p1)
        v-cp (vector-subtract cp p1)
        v-op (vector-subtract op p1)
        cross1 (cross-product-2d v-line v-cp)
        cross2 (cross-product-2d v-line v-op)]
    ;; The sign of cross2 tells us which "side" of the line op is on.
    ;; If cross2 is 0, then op is on the line.
    (if (< (abs cross2) 1)
      ;; In which case, we return true.
      true
      ; Otherwise, check if cp and op have opposite signs (i.e. are on opposite sides of the line).
      (not (pos? (* cross1 cross2))))))

(defn is-behind?
  "Detect if a hex is 'behind' a given hex-side."
  [this-hex other-hex side layout]
  (let [this-pixel (hex/hex->pixel this-hex layout)
        points (hex/points this-hex layout)
        points-list (get-in movement/directions [side :points])
        p1 [(nth points (first points-list)) (nth points (second points-list))]
        p2 [(nth points (nth points-list 2)) (nth points (nth points-list 3))]
        other-hex (hex/hex->pixel other-hex layout)]
    (line-between-points? p1 p2 [(:x this-pixel) (:y this-pixel)] [(:x other-hex) (:y other-hex)])))

(defn ->targeting
  [{:keys [movement pilot attacks heat] :as attacker} target board layout attack]
  (let [atk-hex (movement/get-hex movement board)
        tgt-hex (movement/get-hex (:movement target) board)
        line (board/line atk-hex tgt-hex board)
        range (hex/distance atk-hex tgt-hex)
        attack-data (conj []
                          (pilot/skill-mod pilot)
                          (attacks/calculate-fc-hits attacks)
                          (movement/amm movement)
                          (movement/tmm (:movement target) (:abilities target) (high-heat? target))
                          (when (not (some attack #{:physical :charge :dfa}))
                            (heat/heat-effects heat))
                          (when (height-checker attacker target line)
                            [{:desc "Line of sight blocked" :value ##Inf}])
                          (woods-mod line)
                          (calculate-range-mod range))
        damage (attacks/print-damage attacks attack range)
        targeting {:attacker attacker
                   :target target
                   :attack attack
                   :attack-data attack-data
                   :range range
                   :rear-attack? (is-behind? (get-location target) (get-location attacker) (movement/get-rear (:movement target)) layout)
                   :damage damage}]
    [attack targeting]))

(defn calculate-to-hit
  [{:keys [attack-data]}]
  (let [data (reduce + (map #(get (first %) :value 0) attack-data))]
    data))

(defn attack-roll-parser
  [[m]]
  (if (neg? (:value m 0))
    (str "- " (abs (:value m 0)) " (" (:desc m) ") ")
    (str "+ " (:value m 0) " (" (:desc m) ") ")))

(defn print-attack-roll
  ([attack-roll]
   (print-attack-roll attack-roll true))
  ([{:keys [attack-data] :as attack-roll} detailed?]
   (if-let [failed (some #(= ##Inf (:value (first %))) attack-data)]
     (:desc failed)
     (let [to-hit (calculate-to-hit attack-roll)
           to-hit-str (str "To Hit: " to-hit " (" (get probabilities to-hit) "%)")]
       (if detailed?
         (str/trim (str to-hit-str ": " (reduce str (map attack-roll-parser attack-data))))
         (str/trim to-hit-str))))))

(defn attack-confirmation-choices
  [{:keys [attacks] :as attacker} target board layout]
  (map #(->targeting attacker target board layout %) (keys (attacks/get-attacks attacks))))

(defn set-attacked
  [unit]
  (-> unit
      (take-action)
      (assoc :attacked? true)
      (assoc :targeting false)))

(defn dfa-attack
  [{:keys [attacker target attack-data rear-attack?] :as targeting} to-hit]
  (let [hit? (<= (calculate-to-hit targeting) to-hit)
        attacker (set-attacked attacker)
        attacker-damage (attacks/roll-damage (:attacks attacker) (if hit? :self-dfa :missed-dfa) (movement/tmm (:movement attacker) (:abilities attacker) (high-heat? attacker)) (attacks/get-size target) rear-attack?)
        target-damage (attacks/roll-damage (:attacks attacker) :dfa (movement/tmm (:movement attacker) (:abilities attacker) (high-heat? attacker)) (attacks/get-size target) rear-attack?)
        result {(:id attacker) (damage/take-damage attacker attacker-damage false)
                (:id target) (if hit? (damage/take-damage target target-damage (= to-hit 12)) target)}]
    (mu/log ::dfa-attack
            :hit? hit?
            :targeting-data attack-data
            :to-hit to-hit
            :attacker (:id attacker)
            :attacker-damage attacker-damage
            :target-damage target-damage
            :target (:id target)
            :result result)
    {:targeting-data attack-data
     :to-hit to-hit
     :attacker attacker
     :target-damage target-damage
     :attacker-damage attacker-damage
     :target target
     :result result}))

(defn charge-attack
  [{:keys [attacker target attack-data rear-attack?] :as targeting} to-hit]
  (let [hit? (<= (calculate-to-hit targeting) to-hit)
        attacker (set-attacked attacker)
        attacker-damage (attacks/roll-damage (:attacks attacker) :self-charge (movement/tmm (:movement attacker) (:abilities attacker) (high-heat? attacker)) (attacks/get-size target) rear-attack?)
        target-damage (attacks/roll-damage (:attacks attacker) :charge (movement/tmm (:movement attacker) (:abilities attacker) (high-heat? attacker)) (attacks/get-size target) rear-attack?)
        result {(:id attacker) (if hit? (damage/take-damage attacker attacker-damage false) attacker)
                (:id target) (if hit? (damage/take-damage target target-damage (= to-hit 12)) target)}]
    (mu/log ::charge-attack
            :hit? hit?
            :targeting-data attack-data
            :to-hit to-hit
            :attacker (:id attacker)
            :attacker-damage attacker-damage
            :target-damage target-damage
            :target (:id target)
            :result result)
    {:targeting-data attack-data
     :to-hit to-hit
     :attacker attacker
     :target-damage target-damage
     :attacker-damage attacker-damage
     :target target
     :result result}))

(defn basic-attack
  [{:keys [attacker target attack range rear-attack?] :as atk-data} to-hit]
  (let [damage (attacks/roll-damage (:attacks attacker) attack range rear-attack?)
        result {(:id attacker) (set-attacked attacker)
                (:id target) (if (<= (calculate-to-hit atk-data) to-hit)
                               (damage/take-damage target damage (= to-hit 12))
                               target)}]
    {:targeting-data atk-data
     :to-hit to-hit
     :target-damage damage
     :result result}))

(defn heat-attack
  [{:keys [attacker target attack range rear-attack?] :as atk-data} to-hit]
  (let [damage (attacks/roll-damage (:attacks attacker) attack range rear-attack?)
        result {(:id attacker) (set-attacked attacker)
                (:id target) (if (<= (calculate-to-hit atk-data) to-hit)
                               (damage/heat-damage target damage)
                               target)}]
    {:targeting-data atk-data
     :to-hit to-hit
     :target-damage damage
     :result result}))

(defn make-attack
  ([{:keys [attack] :as atk-data} to-hit]
   (condp = attack
     :charge (charge-attack atk-data to-hit)
     :dfa (dfa-attack atk-data to-hit)
     :heat (heat-attack atk-data to-hit)
     (basic-attack atk-data to-hit)))
  ([attack-data]
   (make-attack attack-data (utils/roll2d))))

(defn can-charge?
  "You can charge a unit if they have acted, you have moved, and they are adjacent to you."
  [{:keys [movement]} target]
  (and (acted? target) (pos? (count (movement/get-path movement))) (= (hex/distance (last (movement/get-path movement)) (get-location target)) 1)))

(defn apply-damage
  [{:keys [damage] :as unit}]
  (assoc unit :damage (damage/apply-damage damage)))

(defn end-phase
  [unit]
  (assoc unit :acted false))

(defn end-phase-heat
  [unit heat overheat-used water? acted? external-heat]
  (assoc unit :heat (heat/end-phase-heat heat overheat-used water? acted? external-heat)))

(defn end-turn
  "Updates damage, applies weapons crits, resets acted, and then returns the unit IF they
  are not destroyed."
  [{:keys [damage heat] :as unit}]
  (let [new-crits (damage/get-new-crits damage)
        weapon-count (count (filter #(= :weapon %) new-crits))
        fc-count (count (filter #(= :fire-control %) new-crits))
        mv-count (count (filter #(= :mv %) new-crits))
        engine-crits (count (filter #(= :engine %) new-crits))
        external-heat (+ engine-crits (damage/get-heat damage))
        new-unit (-> unit
                     (clear-movement)
                     (apply-damage)
                     (take-weapon-hits weapon-count)
                     (take-fc-hits fc-count)
                     (take-mv-hits mv-count)
                     (end-phase-heat heat 0 false (attacked? unit) external-heat)
                     (clear-attacked))]
    (when-not (damage/destroyed? (:damage new-unit))
      {(:id new-unit) new-unit})))
