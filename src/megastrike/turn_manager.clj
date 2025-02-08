(ns megastrike.turn-manager
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.ai.kevin :as ai]
   [megastrike.combat-unit :as cu]
   [megastrike.force :as force]
   [megastrike.hexagons.hex :as hex]
   [megastrike.phases :as phases]))

(defn parse-attack-data
  [{:keys [targeting-data to-hit target-damage result]}]
  (let [atk-id (get-in targeting-data [:attacker :id])
        tgt-id (get-in targeting-data [:target :id])
        target (get result tgt-id)
        crit (cu/get-new-crits target)
        arm (cu/get-current target :armor)
        penetration (- target-damage arm)
        target-num (cu/calculate-to-hit targeting-data)]
    (str atk-id " attacks " tgt-id ". Needs a " target-num ".\n"
         "Rolled a " to-hit "\n"
         (if (<= target-num to-hit)
           (str "Attack hits for " target-damage " damage against " arm " armor.\n"
                (when (pos? penetration)
                  (str penetration " damage penetrates. " (cu/get-current target :structure) " structure remaining.\n"))
                (when (or (= to-hit 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if crit (str crit) "no critical") " on the critical hits table.\n")))
           "Attack misses.\n")
         \newline \newline \newline)))

(defn hex-clicked
  [{:keys [current-phase active-unit units game-board turn-flag layout] :as game-state} hex click-location]
  (mu/log ::turn-flag?
          :turn-flag turn-flag)
  (let [unit (get units active-unit {})]
    (if active-unit
      (cond
        (and (contains? #{:deployment :movement} current-phase) turn-flag)
        (let [unit-location (if (pos? (count (cu/get-path unit)))
                              (last (cu/get-path unit))
                              (cu/get-location unit))
              facing (hex/facing unit-location click-location layout)
              units (update units active-unit cu/set-facing facing)]
          (assoc game-state :units units :turn-flag nil))

        (and (= current-phase :deployment) (not (cu/acted? unit)))
        (update-in game-state [:units active-unit] cu/set-location (select-keys hex [:p :q :r]))

        (and (= current-phase :movement) (not (cu/acted? unit)))
        (update-in game-state [:units active-unit] cu/set-path hex game-board units)
        :else game-state)

      (do (mu/log ::no-active-unit)
          game-state))))

(defn set-movement-mode
  [game-state unit mode]
  (update-in game-state [:units (cu/id unit)] cu/set-movement-mode mode))

(defn cancel-move
  [game-state unit]
  (update-in game-state [:units (cu/id unit)] cu/cancel-movement))

(defn deploy-unit
  [{:keys [active-unit units turn-order] :as game-state}]
  (let [unit (get units active-unit)]
    (if (cu/deployed? unit)
      (do (mu/log ::unit-deployed
                  :unit unit)
          (assoc game-state
                 :turn-order (rest turn-order)
                 :units (assoc units active-unit (cu/take-action unit))
                 :active-unit nil
                 :turn-flag false))
      (do (mu/log ::deployment-failed
                  :unit unit)
          (assoc game-state
                 :active-unit nil
                 :turn-flag false)))))

(defn undeploy-unit
  [{:keys [active-unit] :as game-state}]
  (update-in game-state [:units active-unit] cu/undeploy))

(defn in-active-force?
  [unit turn-order]
  (= (cu/get-force unit) (first turn-order)))

(defn switch-unit
  [{:keys [active-unit units turn-order] :as game-state} new-active-id]
  (let [old-active-unit (get units active-unit {})
        new-active-unit (get units new-active-id)
        active-id (if (and (in-active-force? new-active-unit turn-order) (not (cu/acted? new-active-unit)))
                    new-active-id
                    active-unit)]
    (mu/log ::switching-unit
            :old-active old-active-unit
            :new-active new-active-id
            :active-id active-id)
    (assoc game-state
           :active-unit active-id
           :turn-flag false)))

(defn charge-unit
  [{:keys [active-unit units game-board layout] :as game-state} target]
  (let [unit (get units active-unit)
        mv-type (cu/get-movement unit true)
        can-charge? (cu/can-charge? unit target)
        can-dfa? (and (= mv-type :jump) (cu/can-charge? unit target))
        kind (cond
               can-dfa? :dfa
               can-charge? :charge
               :else :none)]
    (if (not= kind :none)
      (update-in game-state [:internal :attack-dialog] assoc :showing true
                 :items (cu/attack-confirmation-choices unit target game-board layout)
                 :phase :movement
                 :unit unit)
      game-state)))

(defn unit-clicked
  [{:keys [current-phase units active-unit game-board layout turn-order] :as game-state} unit]
  (mu/with-context {:unit-clicked unit :phase current-phase}
    (cond
      (and (in-active-force? unit turn-order) (not (cu/acted? unit)))
      (switch-unit game-state (cu/id unit))

      (and (= current-phase :movement) (not (in-active-force? unit turn-order)))
      (charge-unit game-state unit)

      (and (= current-phase :combat) (not (in-active-force? unit turn-order)))
      (update-in game-state [:internal :attack-dialog] assoc
                 :showing true
                 :items (cu/attack-confirmation-choices (get units active-unit) unit game-board layout)
                 :unit unit)
      :else game-state)))

(defn make-attack
  [{:keys [units round-report] :as game-state} targeting]
  (let [attack-result (cu/make-attack targeting)
        units (merge units (:result attack-result))
        report (str round-report (parse-attack-data attack-result))]
    (assoc game-state :units units :round-report report)))

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
                         (filter #(not (cu/acted? %))))]
      (if (empty? ai-units)
        game-state
        (recur (let [attacker (first ai-units)
                     firing-solutions (ai/targeting-options attacker target-units game-board layout)
                     selected (ai/select-target firing-solutions)]
                 (make-attack game-state selected))
               (rest ai-units))))))

(defn confirm-move
  [{:keys [active-unit units turn-order game-board] :as game-state}]
  (let [unit (get units active-unit)
        moved-unit (if (= (first turn-order) (cu/get-force unit))
                     (cu/move-unit unit game-board)
                     unit)]
    (if (cu/acted? moved-unit)
      (do (mu/log ::move-confirmed
                  :unit moved-unit
                  :destination (cu/get-location moved-unit)
                  :remaining-moves (rest turn-order)
                  :instrumentation :player)
          (take-turn (assoc game-state
                            :turn-order (rest turn-order)
                            :units (assoc units (cu/id moved-unit) moved-unit)
                            :turn-flag nil
                            :active-unit nil)))
      (do (mu/log ::move-failed
                  :origin (cu/get-location moved-unit)
                  :force (cu/get-force moved-unit)
                  :force-conditional (= (first turn-order) (cu/get-force unit))
                  :active unit
                  :path (cu/get-path unit))
          (assoc game-state :turn-flag nil)))))

(defn ai-moves
  [{:keys [turn-order units game-board layout] :as game-state}]
  (let [unit (->> units
                  (vals)
                  (filter #(in-active-force? % turn-order))
                  (filter #(not (cu/acted? %)))
                  (rand-nth))
        opponents (->> units
                       (vals)
                       (filter #(not (in-active-force? % turn-order))))
        move-options (ai/move-options unit opponents game-board layout)
        upd (-> unit
                (cu/set-path (:path move-options))
                (cu/set-movement-mode (cu/get-movement unit true)))]
    (mu/log ::ai-moves
            :move-options move-options
            :upd upd)
    (-> game-state
        (assoc-in [:units (cu/id upd)] upd)
        (assoc :active-unit (cu/id upd))
        (confirm-move))))

(defn take-turn
  [{:keys [forces current-phase turn-order] :as game-state}]
  (let [next-force (get forces (first turn-order))]
    (cond
      (and (= current-phase :combat) (= (force/get-player next-force) :kevin))
      (ai-attacks game-state)
      (and (= current-phase :movement) (= (force/get-player next-force) :kevin))
      (ai-moves game-state)
      :else game-state)))

(defn set-special-attack
  [{:keys [active-unit game-board] :as game-state} targeting]
  (update-in game-state [:units active-unit] cu/declare-special-attack targeting game-board))

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
        targeting-list (map #(cu/->targeting % (get units (:target %)) game-board layout) attackers)]
    (make-attacks game-state targeting-list)))

(defn advance-turn
  [{:keys [turn-order] :as game-state}]
  (if (empty? turn-order)
    (take-turn (phases/next-phase game-state))
    (take-turn (assoc game-state :turn-order (rest turn-order)))))
