(ns megastrike.attacks
  (:require
   [clojure.math :as math]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :as utils]))

(def probabilities
  {2  100, 3  98, 4  92, 5  83, 6  72, 7  58, 8  42, 9  28, 10 17, 11 8, 12 3})

(def criticals {2 :ammo
                3 :engine
                4 :fire-control
                6 :weapon
                7 :mv
                8 :weapon
                10 :fire-control
                11 :engine
                12 :destroyed})

(defn get-tmm
  ([{:keys [tmm crits]}]
   (let [div (count (filter #(= :mv %) crits))]
     (loop [tmm tmm
            n 0]
       (if (= n div)
         tmm
         (recur (let [new-tmm (math/round (/ tmm 2.0))]
                  (if (>= (- tmm new-tmm) 1) new-tmm 0))
                (inc n)))))))

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
    :jump [{:desc "target jumped" :value (inc (get-tmm unit))}]
    [{:desc "target moved" :value (get-tmm unit)}]))

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

(defn produce-attack-roll
  [attacker target board type]
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
        damage (cu/print-damage attacker range (= type :physical))]
    {:targeting attack-roll
     :damage damage}))

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
       (let [crit (first crits)]
         (prn crit)
         (case crit
           :ammo (let [case (str/includes? (:abilities unit) "CASE")
                       case2 (str/includes? (:abilities unit) "CASEII")
                       ene (str/includes? (:abilities unit) "ENE")]
                   (cond (or case2 ene) unit
                         case (take-damage unit 1)
                         :else (assoc-in unit [:changes :destroyed?] true)))
           :engine (if (some #(= % :engine) (cu/get-crits unit))
                     (assoc-in unit [:changes :destroyed?] true)
                     (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :engine)))
           :fire-control (if (< (count (filter #(= % :fire-control) (cu/get-crits unit))) 4)
                           (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :fire-control))
                           unit)
           :weapon (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :weapon))
           :mv (if (< (count (filter #(= % :mv) (cu/get-crits unit))) 4)
                 (assoc-in unit [:changes :crits] (conj (get-in unit [:changes :crits]) :mv))
                 (assoc-in unit [:changes :movement] {:immobile 0}))
           :destroyed (assoc-in unit [:changes :destroyed?] true)
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
       {:crit crits :result upd}))))

(defn attack-confirmation-choices
  [attacker target board]
  (let [range (hex/distance attacker target)
        regular-attack (produce-attack-roll attacker target board :regular)
        physical-attack (produce-attack-roll attacker target board :physical)]
    [{:regular (str (print-attack-roll regular-attack false) ": " (:damage regular-attack) " damage.")}
     (when (= range 1)
       {:regular (str (print-attack-roll physical-attack false) ": " (:damage physical-attack) " damage.")})]))

(defn generate-attack-info
  [units current-force board]
  (loop [ret ""
         attackers (filter #(:target %) current-force)]
    (if (empty? attackers)
      ret
      (let [attacker (first attackers)
            target (get units (:target attacker))
            atk-data (produce-attack-roll attacker target board (:attack attacker))]
        (recur (str ret (:full-name attacker) " will attack " (:full-name target) ": " (calculate-to-hit atk-data) "\n"
                    "Modifiers: " (print-attack-roll atk-data) "\n"
                    "Damage: " (cu/print-damage attacker (hex/distance attacker target) (:physical attacker)) "\n")
               (rest attackers))))))

(defn make-attack
  ([attacker target board layout to-hit]
   (let [targeting-data (produce-attack-roll attacker target board (:attack attacker))
         rear-attack? (detect-direction target attacker (get-in cu/directions [(:direction target) :rear]) layout)
         damage (cu/calculate-damage attacker (hex/distance attacker target) rear-attack?)]
     (mu/log ::make-attack
             :attacker (:id attacker)
             :target (:id target)
             :rear-attack? rear-attack?
             :targeting-data targeting-data
             :to-hit to-hit)
     (merge {:targeting-data targeting-data
             :rear-attack? rear-attack?
             :to-hit to-hit
             :attacker attacker
             :damage damage
             :target target}
            (if (<= (calculate-to-hit targeting-data) to-hit)
              (take-damage target damage (= to-hit 12))
              {:result target}))))
  ([attacker target board layout]
   (make-attack attacker target board layout (utils/roll2d))))
