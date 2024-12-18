(ns megastrike.attacks
  (:require
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :as utils]))

(def probabilities
  {2  100, 3  98, 4  92, 5  83, 6  72, 7  58, 8  42, 9  28, 10 17, 11 8, 12 3})

(def criticals
  {2 :ammo
   3 :engine
   4 :fire-control
   6 :weapon
   7 :mv
   8 :weapon
   10 :fire-control
   11 :engine
   12 :destroyed})

(defn attacker-skill
  [{:keys [pilot]}]
  [{:desc "pilot skill" :value (:skill pilot)}])

(defn calculate-amm
  [{:keys [movement-mode]}]
  (condp = movement-mode
    :immobile [{:desc "attacker immobile" :value -1}]
    :stand-still [{:desc "attacker stood still" :value -1}]
    :jump [{:desc "attacker jumped" :value 2}]
    [{:desc "attacker moved" :value 0}]))

(defn calculate-fc-hits
  [{:keys [crits]}]
  (let [fc (count (filter #(= % :fire-control) crits))
        s (if (= fc 1)
            "fire control hit"
            "fire control hits")]
    [{:desc (str fc " " s) :value (* fc 2)}]))

(defn calculate-target-mod
  [{:keys [movement-mode] :as unit}]
  (condp = movement-mode
    :immobile [{:desc "target immobile" :value -4}]
    :stand-still [{:desc "target did not move" :value 0}]
    :jump [{:desc "target jumped" :value (inc (cu/get-tmm unit))}]
    [{:desc "target moved" :value (cu/get-tmm unit)}]))

(defn calculate-heat-mod
  [{:keys [current-heat] :or {current-heat 0}}]
  (when (pos? current-heat)
    [{:desc "attacker heat" :value current-heat}]))

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

(defn detect-direction
  "Detect if a hex is 'behind' a given hex-side."
  [this-hex other-hex side layout]
  (let [this-pixel (hex/hex->pixel this-hex layout)
        points (hex/points this-hex layout)
        points-list (get-in cu/directions [side :points])
        p1 [(nth points (first points-list)) (nth points (second points-list))]
        p2 [(nth points (nth points-list 2)) (nth points (nth points-list 3))]
        other-hex (hex/hex->pixel other-hex layout)]
    (line-between-points? p1 p2 [(:x this-pixel) (:y this-pixel)] [(:x other-hex) (:y other-hex)])))

(defn produce-attack-roll
  ([attacker target board layout type flag]
   (let [line (board/line attacker target board)
         range (hex/distance attacker target)
         attack-roll (conj [] (attacker-skill attacker)
                           (calculate-fc-hits attacker)
                           (calculate-amm attacker)
                           (calculate-target-mod target)
                           (when (and (not= type :physical) (calculate-heat-mod attacker))
                             (calculate-heat-mod attacker))
                           (if (height-checker attacker target line)
                             [{:desc "Line of Sight Blocked" :value ##Inf}]
                             [{:desc "clear line of sight" :value 0}])
                           (woods-mod line)
                           (calculate-range-mod range))
         damage (cu/print-damage attacker range (= type :physical))
         attack-data {:targeting attack-roll
                      :flag flag
                      :target (:id target)
                      :rear-attack? (detect-direction target attacker (get-in cu/directions [(:direction target) :rear]) layout)
                      :damage damage}]
     attack-data))
  ([attacker target board layout type]
   (produce-attack-roll attacker target board layout type type)))

(defn calculate-to-hit
  [{:keys [targeting]}]
  (reduce + (map #(:value (first %) 0) targeting)))

(defn attack-roll-parser
  [[m]]
  (if (neg? (:value m 0))
    (str "- " (abs (:value m 0)) " (" (:desc m) ") ")
    (str "+ " (:value m 0) " (" (:desc m) ") ")))

(defn print-attack-roll
  ([attack-roll]
   (print-attack-roll attack-roll true))
  ([{:keys [targeting] :as attack-roll} detailed?]
   (if (some #(= ##Inf (:value (first %))) targeting)
     (:desc (first (first (filter #(= ##Inf (:value (first %))) targeting))))
     (let [to-hit (calculate-to-hit attack-roll)
           to-hit-str (str "To Hit: " to-hit " (" (get probabilities to-hit) "%)")]
       (if detailed?
         (str/trim (str to-hit-str ": " (reduce str (map attack-roll-parser targeting))))
         (str/trim to-hit-str))))))

(defn roll-crits
  [tac penetration]
  (let [tac-crit (if tac (get criticals (utils/roll2d) nil) nil)
        pen-crit (if (pos? penetration) (get criticals (utils/roll2d) nil) nil)]
    [tac-crit pen-crit]))

(declare take-damage)

(defn take-crits
  [unit crits]
  (loop [unit unit
         crits crits]
    (if (empty? crits)
      unit
      (recur
       (let [crit (first crits)
             changes (:changes unit)]
         (case crit
           :ammo (let [case-ability (contains? (:abilities unit) :case)
                       case2 (contains? (:abilities unit) :caseii)
                       ene (contains? (:abilities unit) :ene)]
                   (cond (or case2 ene) unit
                         case-ability (take-damage unit 1)
                         :else (assoc unit :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :ammo)))))
           :engine (if (some #(= % :engine) (cu/get-crits unit))
                     (assoc unit :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :engine)))
                     (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :engine)))
           :fire-control (if (< (count (filter #(= % :fire-control) (cu/get-crits unit))) 4)
                           (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :fire-control))
                           unit)
           :weapon (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :weapon))
           :mv (if (< (count (filter #(= % :mv) (cu/get-crits unit))) 4)
                 (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :mv))
                 (assoc-in unit [:changes :movement] {:immobile 0}))
           :destroyed (assoc unit :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :destroyed)))
           unit))
       (rest crits)))))

(defn take-damage
  ([unit damage]
   (take-damage unit damage false))
  ([unit damage tac]
   (if (zero? damage)
     {:crit [nil nil] :result unit}
     (let [armor (max (- (cu/get-armor unit) damage) 0)
           penetration (- damage (cu/get-armor unit))
           structure (if (zero? armor)
                       (- (cu/get-structure unit) penetration)
                       (cu/get-structure unit))
           crits (roll-crits tac penetration)
           upd (if crits
                 (take-crits (assoc unit :changes {:current-armor armor :current-structure structure}) crits)
                 (assoc unit :changes {:current-armor armor :current-structure structure}))]
       (mu/log ::damage-dealt
               :target (:id unit)
               :damage damage
               :crit crits
               :unit-status upd)
       upd))))

(defn create-confirmation-choice
  [attacker target board layout atk-flag atk-class]
  {atk-flag (produce-attack-roll attacker target board layout atk-class atk-flag)})

(defn physical-confirmation-choices
  [attacker target board layout kind]
  (when (not= kind :none)
    [(create-confirmation-choice attacker target board layout kind :physical)]))

(defn attack-confirmation-choices
  [attacker target board layout]
  (let [range (hex/distance attacker target)
        regular-attack (create-confirmation-choice attacker target board layout :regular :regular)
        physical-attack (create-confirmation-choice attacker target board layout :physical :physical)]
    (vec (remove nil? [(when (= range 1) physical-attack) regular-attack]))))

(defn dfa-attack
  [attacker target attack-data to-hit target-damage attacker-damage]
  (let [result {(:id attacker) (if (<= (calculate-to-hit attack-data) to-hit)
                                 (take-damage attacker attacker-damage false)
                                 (take-damage attacker (inc attacker-damage) false))
                (:id target) (if (<= (calculate-to-hit attack-data) to-hit)
                               (take-damage target target-damage (= to-hit 12))
                               target)}]
    (mu/log ::make-dfa-attack
            :targeting-data attack-data
            :to-hit to-hit
            :result result)
    (merge {:targeting-data attack-data
            :to-hit to-hit
            :attacker attacker
            :target-damage target-damage
            :attacker-damage attacker-damage
            :target target
            :result result})))

(defn charge-attack
  [attacker target attack-data to-hit target-damage attacker-damage]
  (let [result {(:id attacker) (if (<= (calculate-to-hit attack-data) to-hit)
                                 (take-damage attacker attacker-damage false)
                                 attacker)
                (:id target) (if (<= (calculate-to-hit attack-data) to-hit)
                               (take-damage target target-damage (= to-hit 12))
                               target)}]
    (mu/log ::make-charge-attack
            :targeting-data attack-data
            :to-hit to-hit
            :result result)
    (merge {:targeting-data attack-data
            :to-hit to-hit
            :attacker attacker
            :target-damage target-damage
            :attacker-damage attacker-damage
            :target target
            :result result})))

(defn basic-attack
  [attacker target attack-data to-hit damage]
  (let [result {(:id attacker) attacker
                (:id target) (if (<= (calculate-to-hit attack-data) to-hit)
                               (take-damage target damage (= to-hit 12))
                               target)}]
    (mu/log ::make-attack
            :targeting-data attack-data
            :to-hit to-hit
            :result result)
    {:targeting-data attack-data
     :to-hit to-hit
     :attacker attacker
     :target-damage damage
     :target target
     :result result}))

(defn make-attack
  ([atk target attack-data to-hit]
   ;; TODO add logic to damage attackers on successful charge attacks
   (let [target-damage (cu/calculate-damage atk (hex/distance atk target) (:rear-attack? attack-data))
         attacker-damage (cu/calc-self-damage atk target)
         attacker (assoc atk :acted true)]
     (condp = (get-in attacker [:attack :flag])
       :charge (charge-attack attacker target attack-data to-hit target-damage attacker-damage)
       :dfa (dfa-attack attacker target attack-data to-hit target-damage attacker-damage)
       (basic-attack attacker target attack-data to-hit target-damage))))
  ([attacker target attack-data]
   (make-attack attacker target attack-data (utils/roll2d))))
