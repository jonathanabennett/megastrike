(ns megastrike.turn-manager
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.ai.kevin :as ai]
   [megastrike.attacks :as attacks]
   [megastrike.combat-unit :as cu]
   [megastrike.damage :as damage]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.phases :as phases]))

(defn unit-updates
  [game-state updates]
  (reduce (fn [current-state [path value]]
            (assoc-in current-state path value))
          game-state
          updates))

(defn parse-attack-data
  [{:keys [combat-result/attacker combat-result/target combat-result/attack
           combat-result/target-number combat-result/crits combat-result/roll
           combat-result/damage combat-result/armor-damage combat-result/penetration]}]
  (str attacker " attacks " target ". Using a " (name attack) " attack. Needs a " target-number ".\n"
       "Rolled a " roll "\n"
       (if (<= target-number roll)
         (str "Attack hits for " damage " damage.\n" armor-damage " damage to armor."
              (when (pos? penetration) (str penetration " damage penetrates the armor. \n"))
              (when (or (= roll 12) (pos? penetration))
                (str "Possible Critical: Rolled " (if crits (str crits) "no critical") " on the critical hits table.\n")))
         "Attack misses.\n")
       \newline \newline \newline))

(defn hex-clicked
  [{:keys [current-phase active-unit units game-board turn-flag layout] :as game-state} hex click-location]
  (mu/log ::turn-flag?
          :turn-flag turn-flag)
  (let [unit (get units active-unit {})]
    (if active-unit
      (cond
        (and (contains? #{:deployment :movement} current-phase) turn-flag)
        (let [unit-location (if (pos? (count (:unit/path unit)))
                              (last (:unit/path unit))
                              (:unit/location unit))
              facing (hex/facing unit-location click-location layout)
              units (update units active-unit movement/change-facing facing)]
          (assoc game-state :units units :turn-flag nil))

        (and (= current-phase :deployment) (not (:unit/acted? unit)))
        (update-in game-state [:units active-unit] movement/set-location (select-keys hex [:hex/p :hex/q :hex/r]))

        (and (= current-phase :movement) (not (:unit/acted? unit)))
        (update-in game-state [:units active-unit] cu/set-path hex game-board units)
        :else game-state)

      (do (mu/log ::no-active-unit)
          game-state))))

(defn set-movement-mode
  [game-state unit mode]
  (assoc-in game-state [:units (:unit/id unit) :move/selected] mode))

(defn cancel-move
  [game-state unit]
  (update-in game-state [:units (:unit/id unit)] movement/cancel-move))

(defn deploy-unit
  [{:keys [active-unit units turn-order] :as game-state}]
  (let [unit (get units active-unit)]
    (if (movement/deployed? unit)
      (do (mu/log ::unit-deployed
                  :unit unit)
          (assoc game-state
                 :turn-order (rest turn-order)
                 :units (assoc-in units [active-unit :unit/acted?] true)
                 :active-unit nil
                 :turn-flag false))
      (do (mu/log ::deployment-failed
                  :unit unit)
          (assoc game-state
                 :active-unit nil
                 :turn-flag false)))))

(defn undeploy-unit
  [{:keys [active-unit] :as game-state}]
  (assoc-in game-state [:units active-unit :location] {}))

(defn in-active-force?
  [unit turn-order]
  (= (:unit/battle-force unit) (first turn-order)))

(defn switch-unit
  [{:keys [active-unit units turn-order] :as game-state} new-active-id]
  (let [new-active-unit (get units new-active-id)
        active-id (if (and (in-active-force? new-active-unit turn-order) (not (:unit/acted? new-active-unit)))
                    new-active-id
                    active-unit)]
    (assoc game-state
           :active-unit active-id
           :turn-flag false)))

(defn charge-unit
  [{:keys [active-unit units game-board layout] :as game-state} target]
  (let [unit (get units active-unit)
        mv-type (movement/selected-or-default unit)
        can-charge? (cu/can-charge? unit target)
        can-dfa? (and (= mv-type :jump) (cu/can-charge? unit target))
        kind (cond
               can-dfa? :dfa
               can-charge? :charge
               :else :none)]
    (if (not= kind :none)
      (update-in game-state [:internal :attack-dialog] assoc :showing true
                 :items (attacks/attack-confirmation-choices unit target game-board layout)
                 :phase :movement
                 :unit unit)
      game-state)))

(defn unit-clicked
  [{:keys [current-phase units active-unit game-board layout turn-order] :as game-state} unit]
  (mu/with-context {:unit-clicked unit :phase current-phase}
    (cond
      (and (in-active-force? unit turn-order) (not (:unit/acted? unit)))
      (switch-unit game-state (:unit/id unit))

      (and (= current-phase :movement) (not (in-active-force? unit turn-order)))
      (charge-unit game-state unit)

      (and (= current-phase :combat) (not (in-active-force? unit turn-order)))
      (update-in game-state [:internal :attack-dialog] assoc
                 :showing true
                 :items (attacks/attack-confirmation-choices (get units active-unit) unit game-board layout)
                 :unit unit)
      :else game-state)))

(defn make-attack
  [{:keys [round-report] :as game-state} targeting]
  (let [result (attacks/make-attack targeting)
        report (str round-report (parse-attack-data result))]
    (-> game-state
        (assoc :round-report report)
        (unit-updates (:combat-results/changes result)))))

(declare take-turn)

(defn ai-attacks
  [{:keys [turn-order units game-board layout] :as game-state}]
  (let [target-units (->> units
                          (vals)
                          (filter #(not (in-active-force? % turn-order))))]
    (loop [game-state game-state
           ai-units (->> units
                         (vals)
                         (filter #(in-active-force? % turn-order))
                         (filter #(not (:unit/acted? %))))]
      (if (empty? ai-units)
        game-state
        (recur (let [attacker (first ai-units)
                     firing-solutions (ai/targeting-options attacker target-units game-board layout)
                     selected (ai/select-target firing-solutions)]
                 (make-attack game-state selected))
               (rest ai-units))))))

(defn confirm-move
  [{:keys [active-unit units turn-order] :as game-state}]
  (let [unit (get units active-unit)
        moved-unit (if (= (first turn-order) (:unit/battle-force unit))
                     (cu/move-unit unit)
                     unit)]
    (if (:unit/acted? moved-unit)
      (do (mu/log ::move-confirmed
                  :unit moved-unit
                  :destination (:unit/location moved-unit)
                  :remaining-moves (rest turn-order)
                  :instrumentation :player)
          (take-turn (assoc game-state
                            :turn-order (rest turn-order)
                            :units (assoc units (:unit/id moved-unit) moved-unit)
                            :turn-flag nil
                            :active-unit nil)))
      (do (mu/log ::move-failed
                  :origin (:unit/location moved-unit)
                  :force (:unit/battle-force moved-unit)
                  :force-conditional (= (first turn-order) (:unit/battle-force unit))
                  :active unit
                  :path (:unit/path unit))
          (assoc game-state :turn-flag nil)))))

(defn ai-moves
  [{:keys [turn-order units game-board layout] :as game-state}]
  (let [unit (->> units
                  (vals)
                  (filter #(in-active-force? % turn-order))
                  (filter #(not (:unit/acted? %)))
                  (rand-nth))
        move-options (ai/move-options unit (vals units) game-board layout)
        upd (-> unit
                (cu/set-path (:path move-options))
                (assoc :unit/selected (if (empty? (:path move-options)) :move/stand-still (:unit/default unit))))]
    (-> game-state
        (assoc-in [:units (:unit/id upd)] upd)
        (assoc :active-unit (:unit/id upd))
        (confirm-move))))

(defn take-turn
  [{:keys [forces current-phase turn-order] :as game-state}]
  (let [next-force (get forces (first turn-order))]
    (cond
      (= next-force nil) game-state
      (and (= current-phase :combat) (= (:unit-group/player next-force) :kevin))
      (ai-attacks game-state)
      (and (= current-phase :movement) (= (:unit-group/player next-force) :kevin))
      (ai-moves game-state)
      :else game-state)))

(defn set-special-attack
  [{:keys [active-unit] :as game-state} targeting]
  (update-in game-state [:units active-unit] cu/declare-special-attack targeting))

(defn make-attacks
  [game-state targeting-list]
  (loop [game-state game-state
         targeting-list targeting-list]
    (if (empty? targeting-list)
      game-state
      (recur
       (let [targeting (first targeting-list)]
         (make-attack game-state targeting))
       (rest targeting-list)))))

(defn resolve-physical-attacks
  [{:keys [units game-board layout turn-order] :as game-state}]
  (let [attackers (filter #(and (contains? #{:charge :dfa} (get % :atk-type false)) (in-active-force? units turn-order)) (vals units))
        targeting-list (map #(attacks/->targeting % (get units (:target %)) game-board layout) attackers)]
    (make-attacks game-state targeting-list)))

(defn advance-turn
  [{:keys [turn-order] :as game-state}]
  (if (empty? turn-order)
    (take-turn (phases/next-phase game-state))
    (take-turn (assoc game-state :turn-order (rest turn-order)))))
