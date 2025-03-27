(ns megastrike.attacks
  "Handles all attack data. Calculates damage numbers, prints damage brackets, calculates to-hit rolls, and produces
  attack options for the GUI to consume."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.string :as string]
   [megastrike.abilities :as abilities]
   [megastrike.board :as board]
   [megastrike.damage :as damage]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(defn ->ranged-attack
  [{:keys [atk-type s s* m m* l l* e e*] :or {atk-type :regular s 0 s* false m 0 m* false l 0 l* false e 0 e* false}}]
  (let [s (if (= (type s) java.lang.String) (Integer/parseInt s) s)
        s* (if (= (type s*) java.lang.String) (= "True" s*) s*)
        m (if (= (type m) java.lang.String) (Integer/parseInt m) m)
        m* (if (= (type m*) java.lang.String) (= "True" m*) m*)
        l (if (= (type l) java.lang.String) (Integer/parseInt l) l)
        l* (if (= (type l*) java.lang.String) (= "True" l*) l*)
        e (if (= (type e) java.lang.String) (Integer/parseInt e) e)
        e* (if (= (type e*) java.lang.String) (= "True" e*) e*)]
    {atk-type {:attack/s s :attack/s* s* :attack/m m :attack/m* m* :attack/l l :attack/l* l*
               :attack/e e :attack/e* e*}}))

(defn ->physical-attack
  [{:keys [atk-type damage self]}]
  {atk-type {:attack/type atk-type :attack/damage damage :attack/self self}})

(defn add-special-attacks
  [abilities]
  (loop [attacks {}
         atk-abilities (filter #(s/valid? :attack/type (first %)) abilities)]
    (if (empty? atk-abilities)
      attacks
      (recur (let [atk (first atk-abilities)]
               (merge attacks (->ranged-attack (merge (second atk) {:atk-type (first atk)}))))
             (rest atk-abilities)))))

(defn ->attacks
  [{:keys [size s s* m m* l l* e e*]} mv-modes abilities]
  (let [size (Integer/parseInt size)]
    (cond-> {}
      true (merge (->ranged-attack {:atk-type :attack/regular
                                    :s s :s* s*
                                    :m m :m* m*
                                    :l l :l* l*
                                    :e e :e* e*}))
      true (merge (->physical-attack
                   {:atk-type :attack/physical
                    :damage (if (abilities/has? abilities :mel)
                              (inc size)
                              size)
                    :self false}))
      true (merge (->physical-attack
                   {:atk-type :attack/charge
                    :damage 0
                    :self true}))
      true (merge (add-special-attacks abilities))
      (movement/has-mode? mv-modes :jump) (merge (->physical-attack
                                                  {:atk-type :attack/dfa
                                                   :damage 0
                                                   :self true})))))

(defn calc-self-charge
  [unit target-size]
  (int (+ (Math/floor (movement/modified-tmm unit)) (if (>= target-size 3) 1 0))))

(defn calc-self-dfa
  [u success?]
  (if success?
    (:unit/size u)
    (inc (:unit/size u))))

(defn calc-charge-damage
  [unit]
  (int (Math/floor (+ (:unit/size unit) (double (/ (movement/modified-tmm unit) 2))))))

(defn calc-dfa-damage
  [unit]
  (inc (calc-charge-damage unit)))

(defn physical-damage
  [unit attack]
  ((condp = (:attack/type attack)
     :attack/charge (calc-charge-damage unit)
     :attack/dfa (calc-dfa-damage unit)
     (:unit/size unit))))

(defn print-damage-bracket
  [attack bracket dmg]
  (let [bracket* (keyword (str (name bracket) "*"))]
    (if (get attack bracket*)
      (if (pos? dmg)
        "0*"
        0)
      (str (max (- (get attack bracket) dmg) 0)))))

(defn print-damage
  [unit attack distance]
  (let [atk (get (:unit/attacks unit) attack)
        dmg (damage/crit-count unit :crits/weapon)]
    (cond
      (and (= distance 1) (or (= attack :attacks/physical) (= attack :attacks/charge) (= attack :attacks/dfa))) (physical-damage unit attack)
      (<= distance 3) (print-damage-bracket atk :attacks/s dmg)
      (<= distance 12) (print-damage-bracket atk :attacks/m dmg)
      (<= distance 21) (print-damage-bracket atk :attacks/l dmg)
      (<= distance 30) (print-damage-bracket atk :attacks/e dmg)
      :else 0)))

(defn ->targeting-mod
  [description value]
  (s/assert :targeting/modifier
            {:targeting/value value
             :targeting/description description}))

(defn woods-mod
  [line]
  (let [woods (count (filter #(string/includes? (:terrain %) "woods") (rest line)))]
    (cond
      (>= woods 3) (->targeting-mod "Line of Sight blocked by woods" ##Inf)
      (string/includes? (:terrain (last line)) "woods") (->targeting-mod "Target in woods" 1)
      (pos? woods) (->targeting-mod "Intervening woods" 1)
      :else (->targeting-mod "No intervening woods" 0))))

(defn calculate-distance-mod
  [distance]
  (let [distance-str (str "Target " distance " hexes away")]
    (condp >= distance
      3  (->targeting-mod distance-str 0)
      12 (->targeting-mod distance-str 2)
      21 (->targeting-mod distance-str 4)
      30 (->targeting-mod distance-str 6)
      (->targeting-mod distance-str ##Inf))))

(defn height-checker
  [origin target line]
  (let [o-height (+ 2 (:elevation (first line)))
        t-height (+ 2 (:elevation (last line)))]
    (if (<= (count line) 2)
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

(defn amm
  [unit]
  (condp = (movement/selected-or-default unit)
    :move/immobilized (->targeting-mod "Attacker immobile" -1)
    :move/stand-still (->targeting-mod "Attacker stood still" -1)
    :move/jump (->targeting-mod "Attacker stood still" 2)
    (->targeting-mod "Attacker moved" 0)))

(defn targeting-tmm
  [unit]
  (->targeting-mod "Target movement" (movement/modified-tmm unit)))

(defn ->targeting
  ([{:keys [attacks] :as attacker} target board layout attack]
   (let [atk-hex (board/find-hex (or (last (:unit/path attacker)) (:unit/location attacker)) board)
         tgt-hex (board/find-hex (:unit/location target) board)
         line (board/line atk-hex tgt-hex board)
         distance (hex/distance atk-hex tgt-hex)
         attack-data (conj []
                           (->targeting-mod "Pilot skill" (get-in attacker [:unit/pilot :pilot/skill]))
                           (->targeting-mod "Fire-control damage" (* (damage/crit-count attacker :crits/fire-control) 2))
                           (amm attacker)
                           (targeting-tmm target)
                           (when (not (some attack #{:attack/physical :attack/charge :attack/dfa}))
                             (->targeting-mod "Attacker heat" (:unit/current-heat attacker)))
                           (when (height-checker attacker target line)
                             (->targeting-mod "Line of sight blocked" ##Inf))
                           (woods-mod line)
                           (calculate-distance-mod distance))
         damage (print-damage attacker attacks attack range)
         targeting {:attacker attacker
                    :target target
                    :attack attack
                    :attack-data attack-data
                    :range distance
                    :rear-attack? (is-behind? (:unit/location target) (or (last (:unit/path attacker)) (:unit/location attacker)) (movement/rear target) layout)
                    :damage damage}]
     [attack targeting]))
  ([{:keys [atk-type] :as attacker} target board layout]
   (->targeting attacker target board layout atk-type)))

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
           to-hit-str (str "To Hit: " to-hit " (" (get utils/probabilities to-hit) "%)")]
       (if detailed?
         (string/trim (str to-hit-str ": " (reduce str (map attack-roll-parser attack-data))))
         (string/trim to-hit-str))))))

(defn attack-confirmation-choices
  [attacker target board layout]
  (map #(->targeting attacker target board layout %) (keys (:unit/attacks attacker))))

(defn roll-damage
  ([unit attack range rear-attack?]
   (let [damage-str (print-damage unit attack range)
         damage (if (and (str/ends-with? damage-str "*") (<= 4 (utils/roll-die)))
                  1
                  (Integer/parseInt damage-str))]
     (if rear-attack?
       (inc damage)
       damage)))
  ([unit attack tmm target-size rear-attack?]
   (let [dmg (condp = attack
               :dfa (calc-dfa-damage unit)
               :charge (calc-charge-damage unit)
               :self-charge (calc-self-charge tmm target-size)
               :self-dfa (calc-self-dfa unit attack)
               :missed-dfa (calc-self-dfa unit attack))]
     (if rear-attack?
       (inc dmg)
       dmg))))
