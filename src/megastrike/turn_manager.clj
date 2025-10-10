(ns megastrike.turn-manager
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.ai.kevin :as ai]
   [megastrike.attacks :as attacks]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]
   [megastrike.phases :as phases]
   [megastrike.battle-force :as battle-force]))

(defn update-units
  "Takes a vector of updates and applies them to the units in the game-state.
  Preconditions: game-state must contain a `units` key which is a vector of maps.
                 updates must be a non-empty vector of maps which contain at least a `unit/id` key
  Postcondition: All changes described in the updates vector will be applied to the units.
  
  @param game-state: The atom containing all the game data
  @param updates: A vector of maps which each must contain a `unit/id` key and then any other keys which should change.
  @return: The updated game-state"
  [game-state updates]
  (loop [units (:units game-state)
         updates updates]
    (if (empty? updates)
      (assoc game-state :units units)
      (recur (cu/update-unit units (first updates))
             (rest updates)))))

(defn parse-attack-data
  [{:keys [combat-result/attacker combat-result/target combat-result/attack
           combat-result/target-number combat-result/crits combat-result/roll
           combat-result/damage combat-result/armor-damage combat-result/penetration]}]
  (str attacker " attacks " target ". Using a " (name attack) " attack. Needs a " target-number ".\n"
       "Rolled a " roll "\n"
       (if (<= target-number roll)
         (if (pos? damage)
           (str "Attack hits for " damage " damage.\n" armor-damage " damage to armor."
                (when (and (int? penetration) (pos? penetration)) (str penetration " damage penetrates the armor. \n"))
                (when (or (= roll 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if crits (str crits) "no critical") " on the critical hits table.\n")))
           "Attack hits for no damage.")

         "Attack misses.\n")
       \newline \newline \newline))

(defn hex-clicked
  [{:keys [current-phase active-unit units game-board turn-flag] :as game-state} layout hex click-location]
  (mu/log ::turn-flag?
          :turn-flag turn-flag)
  (let [unit (or (cu/select-unit units active-unit) {})]
    (if active-unit
      (cond
        (and (contains? #{:deployment :movement} current-phase) turn-flag)
        (let [unit-location (if (pos? (count (:unit/path unit)))
                              (last (:unit/path unit))
                              (:unit/location unit))
              facing (hex/facing unit-location click-location layout)
              units (cu/update-unit units (movement/change-facing unit facing))]
          (assoc game-state :units units :turn-flag nil))

        (and (= current-phase :deployment) (not (:unit/acted? unit)))
        (let [unit (movement/set-location unit (select-keys hex [:hex/p :hex/q :hex/r]))]
          (update-units game-state [unit]))

        (and (= current-phase :movement) (not (:unit/acted? unit)))
        (let [unit (cu/set-path unit hex game-board units)]
          (update-units game-state [unit]))
        :else game-state)

      (do (mu/log ::no-active-unit)
          game-state))))

(defn set-movement-mode
  [game-state unit mode]
  (update-units game-state [{:unit/id (:unit/id unit) :move/selected mode}]))

(defn cancel-move
  [game-state unit]
  (update-units game-state [{:unit/id unit :unit/path [] :unit/selected false}]))

(defn deploy-unit
  [{:keys [active-unit units turn-order] :as game-state}]
  (let [unit (cu/select-unit units active-unit)]
    (if (movement/deployed? unit)
      (do (mu/log ::unit-deployed
                  :unit unit)
          (-> game-state
              (update-units [{:unit/id active-unit :unit/acted? true}])
              (assoc :turn-order (rest turn-order))
              (assoc :active-unit nil)
              (assoc :turn-flag false)))
      (do (mu/log ::deployment-failed
                  :unit unit)
          (assoc game-state
                 :active-unit nil
                 :turn-flag false)))))

(defn undeploy-unit
  [{:keys [active-unit] :as game-state}]
  (update-units game-state [{:unit/id active-unit :unit/location {}}]))

(defn in-active-force?
  [unit turn-order]
  (= (:unit/battle-force unit) (first turn-order)))

(defn switch-unit
  [{:keys [active-unit units turn-order] :as game-state} new-active-id]
  (let [new-active-unit (cu/select-unit units new-active-id)
        active-id (if (and (in-active-force? new-active-unit turn-order) (not (:unit/acted? new-active-unit)))
                    new-active-id
                    active-unit)]
    (assoc game-state
           :active-unit active-id
           :turn-flag false)))

(defn charge-unit
  [{:keys [active-unit units game-board] :as game-state}
   {:keys [layout] :as gui} target]
  (let [unit (cu/select-unit units active-unit)
        mv-type (movement/selected-or-default unit)
        can-charge? (cu/can-charge? unit target)
        can-dfa? (and (= mv-type :jump) (cu/can-charge? unit target))
        kind (cond
               can-dfa? :dfa
               can-charge? :charge
               :else :none)]
    (if (not= kind :none)
      {:game game-state
       :gui (update-in gui [:dialogs :attack-dialog] assoc
                       :showing true
                       :items (attacks/attack-confirmation-choices unit target game-board layout)
                       :phase :movement
                       :unit unit)}
      {:game game-state :gui gui})))

(defn unit-clicked
  [{:keys [current-phase units active-unit game-board turn-order] :as game-state}
   {:keys [layout] :as gui} unit]
  (mu/with-context {:unit-clicked unit :phase current-phase}
    (cond
      (and (in-active-force? unit turn-order) (not (:unit/acted? unit)))
      {:game (switch-unit game-state (:unit/id unit)) :gui gui}

      (and (= current-phase :movement) (not (in-active-force? unit turn-order)))
      (charge-unit game-state gui unit)

      (and (= current-phase :combat) (not (in-active-force? unit turn-order)))
      {:game game-state
       :gui (update-in gui [:dialogs :attack-dialog] assoc
                       :showing true
                       :items (attacks/attack-confirmation-choices (cu/select-unit units active-unit) unit game-board layout)
                       :unit unit)}

      :else {:game game-state :gui gui})))

(defn make-attack
  [{:keys [round-report] :as game-state} targeting]
  (if (not (:unit/acted? (:targeting/attacker targeting)))
    (let [result (attacks/make-attack targeting)
          report (str round-report (parse-attack-data result))]
      (-> game-state
          (assoc :round-report report)
          (update-units (:combat-result/changes result))))
    game-state))

(declare take-turn)

(defn ai-attacks
  [{:keys [turn-order units game-board] :as game-state} layout]
  (let [target-units (filter #(not (in-active-force? % turn-order)) units)]
    (loop [game-state game-state
           ai-units (->> units
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
  [{:keys [active-unit units turn-order] :as game-state} layout]
  (let [unit (cu/select-unit units active-unit)
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
                            :units (cu/update-unit units moved-unit)
                            :turn-flag nil
                            :active-unit nil) layout))
      (do (mu/log ::move-failed
                  :origin (:unit/location moved-unit)
                  :force (:unit/battle-force moved-unit)
                  :force-conditional (= (first turn-order) (:unit/battle-force unit))
                  :active unit
                  :path (:unit/path unit))
          (assoc game-state :turn-flag nil)))))

(defn ai-moves
  [{:keys [turn-order units game-board] :as game-state} layout]
  (let [unit (->> units
                  (filter #(in-active-force? % turn-order))
                  (filter #(not (:unit/acted? %)))
                  (rand-nth))
        move-options (ai/move-options unit units game-board layout)
        upd (-> unit
                (cu/set-path (:path move-options))
                (assoc :move/selected (if (empty? (:path move-options)) :move/stand-still (:move/default unit))))]
    (-> game-state
        (assoc :units (cu/update-unit units upd))
        (assoc :active-unit (:unit/id upd))
        (confirm-move layout))))

(defn take-turn
  [{:keys [forces current-phase turn-order] :as game-state} layout]
  (let [next-force (battle-force/select-force forces (first turn-order))]
    (cond
      (= next-force nil) game-state
      (and (= current-phase :combat) (= (:unit-group/player next-force) :kevin))
      (ai-attacks game-state layout)
      (and (= current-phase :movement) (= (:unit-group/player next-force) :kevin))
      (ai-moves game-state layout)
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
  [{:keys [units game-board turn-order] :as game-state} layout]
  (let [attackers (filter #(and (contains? #{:charge :dfa} (get % :atk-type false)) (in-active-force? units turn-order)) (vals units))
        targeting-list (map #(attacks/->targeting % (cu/select-unit units (:target %)) game-board layout) attackers)]
    (make-attacks game-state targeting-list)))

(defn advance-turn
  [{:keys [turn-order] :as game-state} layout]
  (if (empty? turn-order)
    (take-turn (phases/next-phase game-state) layout)
    (take-turn (assoc game-state :turn-order (rest turn-order)) layout)))

