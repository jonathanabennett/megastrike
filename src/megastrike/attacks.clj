(ns megastrike.attacks
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [com.brunobonacci.mulog :as mu]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]
            [megastrike.logs :as reports]
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
  ([unit]
   (let [div (count (filter #(= :mv %) (:crits unit)))]
     (loop [tmm (get unit :tmm)
            n 0]
       (if (= n div)
         tmm
         (recur (let [new-tmm (math/round (/ tmm 2.0))]
                  (if (>= (- tmm new-tmm) 1) new-tmm 0))
                (inc n)))))))

(defn attacker-skill
  [unit]
  [{:desc "pilot skill" :value (get-in unit [:pilot :skill])}])

(defn calculate-amm
  [unit]
  (condp = (:movement-mode unit)
    :immobile [{:desc "attacker immobile" :value -1}]
    :stand-still [{:desc "attacker stood still" :value -1}]
    :jump [{:desc "attacker jumped" :value 2}]
    [{:desc "attacker moved" :value 0}]))

(defn calculate-fc-hits
  [unit]
  (let [fc (count (filter #(= % :fire-control) (:crits unit)))
        s (if (= fc 1)
            "fire control hit"
            "fire control hits")]
      [{:desc (str fc " " s) :value (* fc 2)}]))

(defn calculate-target-mod
  [unit]
  (condp = (:movement-mode unit)
    :immobile [{:desc "target immobile" :value -4}]
    :stand-still [{:desc "target did not move" :value 0}]
    :jump [{:desc "target jumped" :value (inc (get-tmm unit))}]
    [{:desc "target moved" :value (get-tmm unit)}]))

(defn calculate-heat-mod 
  [unit]
  (let [heat (get unit :current-heat 0)]
    (when (pos? heat)
      [{:desc "attacker heat" :value heat}])))

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
  [attacker target board]
  (let [line (board/hex-line attacker target board) 
        attack-roll (conj [] (attacker-skill attacker) 
                             (calculate-fc-hits attacker) 
                             (calculate-amm attacker) 
                             (calculate-target-mod target) 
                             (calculate-heat-mod attacker) 
                             (if (height-checker attacker target line) 
                               [{:desc "Line of Sight Blocked" :value ##Inf}]
                               [{:desc "clear line of sight" :value 0}])
                             (woods-mod line) 
                             (calculate-range-mod (hex/hex-distance attacker target)))]
    attack-roll))

(defn calculate-to-hit 
  [attack-roll]
  (reduce + (map #(:value (first %) 0) attack-roll)))

(defn attack-roll-parser 
  [m]
    (let [m (first m)
          s (if (neg? (:value m 0))
              (str "- " (abs (:value m 0)) " (" (:desc m) ") ")
              (str "+ " (:value m 0) " (" (:desc m) ") "))]
      s))

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
  (let [this-pixel (hex/hex-to-pixel this-hex layout)
        points (hex/hex-points this-hex layout)
        points-list (get-in cu/directions [side :points])
        p1 [(nth points (first points-list)) (nth points (second points-list))]
        p2 [(nth points (nth points-list 2)) (nth points (nth points-list 3))]
        other-hex (hex/hex-to-pixel other-hex layout)]
    (line-between-points? p1 p2 [(:x this-pixel) (:y this-pixel)] [(:x other-hex) (:y other-hex)])))

(defn print-attack-roll 
  ([attack-roll]
   (print-attack-roll attack-roll true))
  ([attack-roll detailed?]
   (if (some #(= ##Inf (:value (first %))) attack-roll)
     (:desc (first (first (filter #(= ##Inf (:value (first %))) attack-roll))))
     (let [to-hit (calculate-to-hit attack-roll) 
           to-hit-str (str "To Hit: " to-hit " (" (get probabilities to-hit) "%)")] 
       (if detailed?  
         (let [details (map attack-roll-parser attack-roll)]
           (str/trim (str to-hit-str ": " (reduce str details))))
         (str/trim to-hit-str))))))

(defn take-damage
  ([unit damage]
   (take-damage unit damage false))
  ([unit damage tac]
   (if (= damage 0)
     unit
     (let [armor (max (- (:current-armor unit (:armor unit)) damage) 0) 
           penetration (- damage (:current-armor unit (:armor unit)))
           structure (if (zero? armor) 
                       (- (:current-structure unit (:structure unit)) penetration)
                       (:current-structure unit (:structure unit)))
           crit (if (or tac (pos? penetration)) (get criticals (utils/roll2d) nil) nil)
           damaged (assoc unit :current-armor armor :current-structure structure)
           upd (cond
                 (not (pos? (:current-structure damaged (:structure damaged)))) (assoc damaged :destroyed? true)
                 (= crit :ammo) (let [case (str/includes? (:abilities damaged) "CASE")
                                      case2 (str/includes? (:abilities damaged) "CASEII")
                                      ene (str/includes? (:abilities damaged) "ENE")]
                                  (cond (or case2 ene) damaged
                                        case (take-damage damaged 1)
                                        :else (assoc damaged :destroyed? true :crits (conj (:crits damaged) :ammo))))
                 (= crit :engine) (if (some #(= % :engine) (:crits damaged))
                                    (assoc damaged :destroyed? true)
                                    (assoc damaged :crits (conj (:crits damaged) :engine)))
                 (= crit :fire-control) (if (< (count (filter #(% :fire-control) (:crits damaged))) 4)
                                          (assoc damaged :crits (conj (:crits damaged) :fire-control))
                                          damaged)
                 (= crit :weapon) (if (< (count (filter #(% :weapon) (:crits damaged))) 4)
                                    (cu/take-weapon-hit damaged)
                                    damaged)
                 (= crit :mv) (if (< (count (filter #(% :mv) (:crits damaged))) 4)
                                (assoc damaged :crits (conj (:crits damaged) :mv))
                                (assoc damaged :movement {:immobile 0}))
                 (= crit :destroyed) (assoc damaged :destroyed? true :crits (conj (:crits damaged) :destroyed))
                 :else damaged)]
       (mu/log ::damage-dealt
               :target (:id unit)
               :damage damage
               :crit crit
               :unit-status upd)
       upd))))

(defn attack-confirmation-choices
  [attacker target board]
  (let [atk-data (produce-attack-roll attacker target board)
        range (hex/hex-distance attacker target)
        regular-damage (cu/print-damage attacker range false)
        physical-damage (cu/print-damage attacker range true)]
    [{:regular (str (print-attack-roll atk-data false) ": " regular-damage " damage")}
     (when (= range 1)
       {:physical (str (print-attack-roll atk-data false) ": " physical-damage " damage")})]))

(defn generate-attack-info
  [units current-force board]
  (loop [ret ""
         attackers (filter #(:target %) current-force)]
    (if (empty? attackers)
      ret
      (let [attacker (first attackers)
            target (get units (:target attacker))
            atk-data (produce-attack-roll attacker target board)]
        (recur (str ret (:full-name attacker) " will attack " (:full-name target) ": " (calculate-to-hit atk-data) "\n"
                    "Modifiers: " (print-attack-roll atk-data) "\n"
                    "Damage: " (cu/print-damage attacker (hex/hex-distance attacker target) (:physical attacker)) "\n")
               (rest attackers))))))

(defn make-attack 
  [attacker target board layout]
  (let [targeting-data (produce-attack-roll attacker target board)
        rear-attack? (detect-direction target attacker (get-in cu/directions [(:direction target) :rear]) layout)
        damage (cu/calculate-damage attacker (hex/hex-distance attacker target) rear-attack?)
        to-hit (utils/roll2d)]
    (mu/log ::make-attack
            :attacker (:id attacker)
            :target (:id target)
            :rear-attack? rear-attack?
            :targeting-data targeting-data
            :to-hit to-hit)
    (if (<= (calculate-to-hit targeting-data) to-hit)
      (take-damage target damage (= to-hit 12)) 
      target)))