(ns megastrike.combat-unit
  (:require
   [clojure-csv.core :as csv]
   [clojure.math :as math]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.attacks :as attacks]
   [megastrike.board :as board]
   [megastrike.damage :as damage]
   [megastrike.heat :as heat]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.pilot :as pilot]
   [megastrike.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUL/Core

(defn pv-mod
  "Calculates the skill-based mod for PV based on the algorithm provided in the book."
  [{:keys [unit/pilot unit/base-pv]}]
  (let [skill-diff (- 4 (pilot/skill pilot))]
    (cond
      (> 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- base-pv 5) 10)))
      (< 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- base-pv 3) 5)))
      :else 0)))

(defn pv
  "Returns the modified PV."
  [{:keys [unit/base-pv] :as unit}]
  (+ base-pv (pv-mod unit)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PILOT

(defn set-stacking
  "Mark all units on the board."
  [board units]
  (let [b (board/set-stacking board (for [u units] [(:unit/location u) (:unit/battle-force u)]))]
    (prn (map :stacking b))
    b))

(defn set-path
  ([unit hex board units]
   (movement/set-path unit hex (set-stacking board (vals units))))
  ([unit path]
   (assoc unit :unit/path path)))

(defn move-unit
  [unit]
  (if (movement/can-move? unit (:unit/path unit))
    (-> unit
        (assoc :unit/acted? true)
        (movement/move-unit))
    unit))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ATTACK ACCESS

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Damage Access

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

(defn take-damage
  [{:keys [damage abilities] :as unit} damage-num crit?]
  (assoc unit :damage (damage/take-damage damage abilities damage-num crit?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ABILITIES

(defn- move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :move/walk
      (= mv-key (utils/keyword-maker "j")) :move/jump
      :else (keyword "move" (-> mv-type
                                (string/trim)
                                (string/lower-case)
                                (utils/remove-parens)
                                (utils/correct-range-brackets)
                                (utils/replace-spaces))))))

(defn parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :move/jump))
      (merge mv-map {:move/walk (val (first mv-map))})
      mv-map)))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map #(keyword (utils/keyword-maker %))
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-row
  "Parses a single row of the MUL file."
  ([row]
   (parse-row header-row row))
  ([hr row]
   (let [mul-row (zipmap hr row)
         modes (parse-movement (:movement mul-row))
         abilities (abilities/parse-abilities (:abilities mul-row))]
     (s/assert :unit/mul
               {:unit/chassis (:chassis mul-row)
                :unit/model (:model mul-row)
                :unit/role (keyword "role" (utils/keyword-maker (:role mul-row)))
                :unit/type (keyword "type" (utils/keyword-maker (:type mul-row)))
                :unit/threshold (Integer/parseInt (:threshold mul-row))
                :unit/full-name (str (:chassis mul-row) " " (:model mul-row))
                :unit/mul-id (Integer/parseInt (:mul-id mul-row))
                :unit/size (Integer/parseInt (:size mul-row))
                :unit/move-modes modes
                :unit/structure {:toughness/current (Integer/parseInt (:armor mul-row))
                                 :toughness/maximum (Integer/parseInt (:armor mul-row))
                                 :toughness/unapplied 0}
                :unit/armor {:toughness/current (Integer/parseInt (:armor mul-row))
                             :toughness/maximum (Integer/parseInt (:armor mul-row))
                             :toughness/unapplied 0}
                :unit/tmm (Integer/parseInt (:tmm mul-row))
                :unit/attacks (attacks/->attacks mul-row modes abilities)
                :unit/damage (damage/->damage mul-row)
                :unit/overheat (Integer/parseInt (:overheat mul-row))
                :unit/abilities abilities
                :unit/base-pv (Integer/parseInt (:point-value mul-row))}))))

(def mul
  (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-mechset-line
  "Parses a single line from a mechset file. Mechset files define which images match which units."
  [line]
  (when-not (or (= (string/index-of line "#") 0)
                (= line "")
                (= (string/index-of line "include") 0))
    (let [first-break (string/index-of line " ")
          second-break (string/index-of line "\" " (inc first-break))
          mechset-type (string/trim (subs line 0 first-break))
          search-term (string/trim (utils/strip-quotes (subs line first-break second-break)))
          file-path (string/trim (utils/strip-quotes (subs line second-break)))]
      (vector mechset-type search-term file-path))))

(defn parse-mechset
  "Parses a full Mechset file."
  []
  (into [] (remove
            nil?
            (map #(parse-mechset-line %)
                 (string/split-lines (slurp (utils/load-resource :data "images/units/mechset.txt")))))))

(def mechset (parse-mechset))

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [{:keys [unit/chassis unit/full-name]}]
  (let [chassis-match (filter (fn [row] (= (second row) chassis)) mechset)
        exact-match (filter (fn [row] (string/includes? (second row) full-name)) mechset)
        match-row (or (first exact-match) (first chassis-match))]
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

(defn ->combat-unit
  ([mul-unit pilot facing location battle-force number]
   (s/assert :unit/combat-unit
             (merge mul-unit
                    {:unit/id (if (pos? number)
                                (str (:unit/full-name mul-unit) " #" (inc number))
                                (:full-name mul-unit))
                     :unit/battle-force battle-force
                     :unit/pilot pilot
                     :unit/facing facing
                     :unit/location location
                     :unit/criticals {:crits/taken [] :crits/unapplied []}
                     :unit/selected nil
                     :unit/default (if (contains? (:unit/move-modes mul-unit) :move/walk) :move/walk (first (keys (:unit/move-modes mul-unit))))
                     :unit/sprite (find-sprite mul-unit)})))
  ([{:keys [units mul-unit pilot battle-force facing location] :or {facing :direction/none location {}}}]
   (->combat-unit mul-unit pilot battle-force facing location (count (filter #(= (:unit/full-name %) (:unit/full-name mul-unit)) units)))))

(defn filter-membership-helper
  "Returns true if a unit matches one of the types."
  ([unit]
   unit)
  ([unit field values]
   (some #(= (field unit) %) values)))

(defn filter-units
  "Filters units based on either a string or a seq of unit type."
  ([units]
   units)
  ([units field value comparison]
   (filter #(when (comparison (field %) value) %) units))
  ([units field values]
   (filter #(filter-membership-helper % field values) units)))

(defn get-unit
  ([s]
   (let [non-standard (string/replace s #"\(Standard\)" "")
         matching-muls (filter-units mul :unit/full-name s =)

         non-standard-mul (filter-units mul :unit/full-name non-standard =)]
     (if (first matching-muls)
       (first matching-muls)
       (first non-standard-mul)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MAKE ATTACKS

(defn attacked?
  [unit]
  (:attacked? unit))

(defn clear-attacked
  [unit]
  (assoc unit :attacked false))

(defn ->targeting-mod
  [description value]
  [{:desc description :value value}])

(defn woods-mod
  [line]
  (let [woods (count (filter #(string/includes? (:terrain %) "woods") (rest line)))]
    (cond
      (>= woods 3) (->targeting-mod "Line of Sight blocked by woods" ##Inf)
      (string/includes? (:terrain (last line)) "woods") (->targeting-mod "Target in woods" 1)
      (pos? woods) (->targeting-mod "Intervening woods" 1)
      :else (->targeting-mod "No intervening woods" 0))))

(defn calculate-range-mod
  [range]
  (let [range-str (str "target " range " hexes away")]
    (condp >= range
      3  (->targeting-mod range-str 0)
      12 (->targeting-mod range-str 2)
      21 (->targeting-mod range-str 4)
      30 (->targeting-mod range-str 6)
      (->targeting-mod range-str ##Inf))))

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
    :immobile (->targeting-mod "Attacker immobile" -1)
    :stand-still (->targeting-mod "Attacker stood still" -1)
    :jump (->targeting-mod "Attacker stood still" 2)
    (->targeting-mod "Attacker moved" 0)))

(defn targeting-tmm
  [unit]
  (->targeting-mod "Target movement" (movement/modified-tmm unit)))

(defn ->targeting
  ([{:keys [attacks] :as attacker} target board layout attack]
   (let [atk-hex (board/find-hex (or (last (:unit/path attacker)) (:unit/location attacker)) board)
         tgt-hex (board/find-hex (:unit/location target) board)
         line (board/line atk-hex tgt-hex board)
         range (hex/distance atk-hex tgt-hex)
         attack-data (conj []
                           (->targeting-mod "Pilot skill" (pilot/skill (:unit/pilot attacker)))
                           (->targeting-mod "Fire-control damage" (* (attacks/fc-hits attacks) 2))
                           (amm attacker)
                           (targeting-tmm target)
                           (when (not (some attack #{:physical :charge :dfa}))
                             (->targeting-mod "Attacker heat" (:unit/current-heat attacker)))
                           (when (height-checker attacker target line)
                             (->targeting-mod "Line of sight blocked" ##Inf))
                           (woods-mod line)
                           (calculate-range-mod range))
         damage (attacks/print-damage attacks attack range)
         targeting {:attacker attacker
                    :target target
                    :attack attack
                    :attack-data attack-data
                    :range range
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
  [{:keys [attacks] :as attacker} target board layout]
  (map #(->targeting attacker target board layout %) (keys (attacks/get-attacks attacks))))

(defn set-attacked
  [unit]
  (-> unit
      (assoc :unit/acted? true)
      (assoc :targeting false)))

(defn declare-special-attack
  [unit targeting]
  (-> unit
      (assoc :target (:unit/id (:target targeting)))
      (assoc :atk-type (:attack targeting))
      (move-unit)))

(defn dfa-attack
  [{:keys [attacker target rear-attack?] :as targeting} to-hit]
  (let [hit? (<= (calculate-to-hit targeting) to-hit)
        attacker (set-attacked attacker)
        attacker-tmm (movement/modified-tmm attacker)
        attacker-damage (attacks/roll-damage (:attacks attacker) (if hit? :self-dfa :missed-dfa) attacker-tmm (:unit/size attacker) rear-attack?)
        target-damage (attacks/roll-damage (:attacks attacker) :dfa attacker-tmm (:unit/size target) rear-attack?)
        result {(:id attacker) (take-damage attacker attacker-damage false)
                (:id target) (if hit? (take-damage target target-damage (= to-hit 12)) target)}]
    (mu/log ::dfa-attack
            :hit? hit?
            :targeting-data targeting
            :to-hit to-hit
            :attacker (:unit/id attacker)
            :attacker-damage attacker-damage
            :target-damage target-damage
            :target (:unit/id target)
            :result result)
    {:targeting-data targeting
     :to-hit to-hit
     :attacker attacker
     :target-damage target-damage
     :attacker-damage attacker-damage
     :target target
     :result result}))

(defn charge-attack
  [{:keys [attacker target rear-attack?] :as targeting} to-hit]
  (let [hit? (<= (calculate-to-hit targeting) to-hit)
        attacker (set-attacked attacker)
        attacker-tmm (movement/modified-tmm attacker)
        attacker-damage (attacks/roll-damage (:attacks attacker) :self-charge attacker-tmm (:unit/size target) rear-attack?)
        target-damage (attacks/roll-damage (:attacks attacker) :charge attacker-tmm (:unit/size target) rear-attack?)
        result {(:id attacker) (if hit? (take-damage attacker attacker-damage false) attacker)
                (:id target) (if hit? (take-damage target target-damage (= to-hit 12)) target)}]
    (mu/log ::charge-attack
            :hit? hit?
            :targeting-data targeting
            :to-hit to-hit
            :attacker (:unit/id attacker)
            :attacker-damage attacker-damage
            :target-damage target-damage
            :target (:unit/id target)
            :result result)
    {:targeting-data targeting
     :to-hit to-hit
     :attacker attacker
     :target-damage target-damage
     :attacker-damage attacker-damage
     :target target
     :result result}))

(defn basic-attack
  [{:keys [attacker target attack distance rear-attack?] :as atk-data} to-hit]
  (let [damage (attacks/roll-damage (:attacks attacker) attack distance rear-attack?)
        result {(:unit/id attacker) (set-attacked attacker)
                (:unit/id target) (if (<= (calculate-to-hit atk-data) to-hit)
                                    (take-damage target damage (= to-hit 12))
                                    target)}]
    {:targeting-data atk-data
     :to-hit to-hit
     :target-damage damage
     :result result}))

(defn heat-attack
  [{:keys [attacker target attack distance rear-attack?] :as atk-data} to-hit]
  (let [damage (attacks/roll-damage (:attacks attacker) attack distance rear-attack?)
        result {(:unit/id attacker) (set-attacked attacker)
                (:unit/id target) (if (<= (calculate-to-hit atk-data) to-hit)
                                    (assoc target :damage (damage/heat-damage target damage))
                                    target)}]
    {:targeting-data atk-data
     :to-hit to-hit
     :target-damage damage
     :result result}))

(defn make-attack
  ([{:keys [attack] :as atk-data} to-hit]
   (mu/log ::making-attack
           :atk-data atk-data)
   (condp = attack
     :charge (charge-attack atk-data to-hit)
     :dfa (dfa-attack atk-data to-hit)
     :heat (heat-attack atk-data to-hit)
     (basic-attack atk-data to-hit)))
  ([attack-data]
   (make-attack attack-data (utils/roll2d))))

(defn can-charge?
  "You can charge a unit if they have acted, you have moved, and they are adjacent to you."
  [attacker target]
  (and (:unit/acted? target) (pos? (count (:unit/path attacker))) (= (hex/distance (last (:unit/path attacker)) (:unit/location target)) 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of Phase and end of turn functions

(defn apply-damage
  [{:keys [damage] :as unit}]
  (assoc unit :damage (damage/apply-damage damage)))

(defn end-phase
  [unit]
  (assoc unit :acted false))

(defn end-phase-heat
  [unit heat overheat-used water? acted? external-heat]
  (assoc unit :heat (heat/end-phase-heat heat overheat-used water? acted? external-heat)))
;; (count (filter #(= :fire-control %) new-crits))
(defn end-turn
  "Updates damage, applies weapons crits, resets acted, and then returns the unit IF they
  are not destroyed."
  [{:keys [damage heat] :as unit}]
  (let [new-crits (damage/get-new-crits damage)
        weapon-count (count (filter #(= :weapon %) new-crits))
        engine-crits (count (filter #(= :engine %) new-crits))
        external-heat (+ engine-crits (damage/get-heat damage))
        new-unit (-> unit
                     (assoc :unit/selected nil)
                     (apply-damage)
                     (take-weapon-hits weapon-count)
                     (end-phase-heat heat 0 false (attacked? unit) external-heat)
                     (clear-attacked))]
    (when-not (damage/destroyed? (:damage new-unit))
      {(:unit/id new-unit) new-unit})))
