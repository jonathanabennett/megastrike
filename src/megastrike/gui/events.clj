(ns megastrike.gui.events
  (:require
   [cljfx.api :as fx]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [com.brunobonacci.mulog :as mu]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.reports :as reports]
   [megastrike.gui.subs :as subs]
   [megastrike.hexagons.hex :as hex]
   [megastrike.logs :as logs]
   [megastrike.phases :as initiative]
   [megastrike.utils :as utils])
  (:import
   [javafx.application Platform]
   [javafx.scene.input MouseEvent]))

;; Defaults and common operations
(defmulti event-handler :event-type)

(defmethod event-handler ::no-op
  [_])

(defmethod event-handler :default
  [{:keys [event-type] :as event}]
  (prn "EVENT!")
  (mu/log ::unhandled-event
          :event-type event-type
          :keys (keys event)))

;; board events
(defn- facing-change
  [unit event layout]
  (let [e ^MouseEvent event
        u (if (pos? (count (cu/get-path unit)))
            (last (cu/get-path unit))
            (cu/get-location unit))
        dest {:x (.getX e) :y (.getY e)}
        facing (hex/facing u dest layout)]
    {:dispatch {:event-type ::change-facing :unit unit :facing facing}}))

(defmethod event-handler ::hex-clicked
  [{:keys [fx/context hex fx/event]}]
  (let [phase (subs/phase context)
        active (subs/active-id context)
        units (subs/units context)
        unit (subs/active-unit context)
        next-force (first (subs/turn-order context))]
    (if unit
      (cond
        (and (some #{phase} [:deployment :movement]) (not (nil? unit)) (fx/sub-val context :turn-flag))
        (facing-change unit event (subs/layout context))
        (and (= phase :deployment) (not (cu/acted? unit)) (= (cu/get-force unit) next-force))
        (let [updated (cu/set-location unit (select-keys hex [:p :q :r]))
              new-units (assoc units active updated)]
          {:context (fx/swap-context context assoc :units new-units)})
        (and (= phase :movement) (not (cu/acted? unit)) (= (cu/get-force unit) next-force))
        (let [updated (cu/set-path unit hex (subs/board context))
              new-units (assoc units active updated)]
          {:context (fx/swap-context context assoc :units new-units)}))
      (mu/log ::no-active-unit))))

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::change-size
  [{:keys [fx/context direction]}]
  (let [layout (subs/layout context)
        new-layout (if (= direction :plus)
                     (assoc layout :scale (+ (:scale layout) 0.1))
                     (assoc layout :scale (- (:scale layout) 0.1)))]
    {:context (fx/swap-context context assoc :layout new-layout)}))

;; Saving, loading, and Phases
(defmethod event-handler ::auto-save
  [{:keys [fx/context]}]
  (let [save {:game-board (subs/board context)
              :units (subs/units context)
              :forces (subs/forces context)}]
    (mu/log ::auto-save-event)
    (pprint/pprint save (io/writer (utils/load-resource :data "save.edn")))))

(defmethod event-handler ::quit-game
  [_]
  (logs/logs)
  (Platform/exit))

(defmethod event-handler ::open-round-dialog
  [{:keys [fx/context]}]
  (let [ctx (get context :round-dialog)
        advance-phase? (empty? (subs/turn-order context))]
    {:context (fx/swap-context context assoc :round-dialog
                               (assoc ctx :showing true :advance-phase? advance-phase?))}))

(defmethod event-handler ::close-round-dialog
  [{:keys [fx/context]}]
  (let [ctx (get context :round-dialog)]
    (if (get ctx :advance-phase? false)
      {:context (fx/swap-context context assoc :round-dialog {:showing false :advance-phase? false})
       :dispatch {:event-type ::next-phase}}
      {:context (fx/swap-context context assoc :round-dialog {:showing false :advance-phase? false})})))

(defmethod event-handler ::next-phase
  [{:keys [fx/context]}]
  (let [phase (subs/phase context)
        turn-number (subs/turn-number context)
        forces (subs/forces context)
        units (subs/units context)
        response (initiative/next-phase {:current-phase phase
                                         :turn-number turn-number
                                         :forces forces
                                         :units units
                                         :round-report (subs/round-report context)})]
    {:context (fx/swap-context context merge response)
     :dispatch {:event-type ::open-round-dialog}}))

;; Unit selection
(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  (let [u (get (subs/units context) unit)]
    (when (and (= (cu/get-force u) (first (subs/turn-order context))) (not (:acted u)))
      {:context (fx/swap-context context assoc :active-unit unit)})))

(defn charge-unit
  [context unit target board layout]
  (let [mv-type (cu/get-movement unit false)
        can-charge? (cu/can-charge? unit target)
        can-dfa? (and (= mv-type :jump) (cu/can-charge? unit target))
        kind (cond
               can-dfa? :dfa
               can-charge? :charge
               :else :none)
        ctx (get-in context [:internal :attack-dialog])]
    (when (not= kind :none)
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog]
                                 (assoc ctx :showing true
                                        :items (cu/attack-confirmation-choices unit target board layout)
                                        :phase :movement
                                        :unit unit))})))

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
  (let [phase (subs/phase context)
        active-force (first (subs/turn-order context))
        active-unit (subs/active-unit context)
        board (subs/board context)
        layout (subs/layout context)]
    (mu/with-context {:unit-clicked unit :phase phase}
      (cond
        (and (= active-force (cu/get-force unit)) (not (cu/acted? unit)))
        {:context (fx/swap-context context assoc :active-unit (cu/id unit))}
        (and (= phase :movement) (not (= active-force (cu/get-force unit))))
        (charge-unit context active-unit unit board layout)
        (and (= phase :combat) (not (= active-force (cu/get-force unit))))
        (let [ctx (get-in context [:internal :attack-dialog])]
          {:context (fx/swap-context context assoc-in [:internal :attack-dialog]
                                     (assoc ctx :showing true
                                            :items (cu/attack-confirmation-choices active-unit unit board layout)
                                            :unit unit))})))))

;; Initiative Phase
(defmethod event-handler ::roll-initiative
  [{:keys [fx/context]}]
  (let [forces (initiative/roll-initiative (subs/forces context))
        turn-order (initiative/generate-turn-order forces (vals (subs/units context)))]
    {:context (fx/swap-context context merge
                               {:forces forces
                                :turn-order turn-order
                                :current-phase :deployment})}))

;; Deployment Phase
(defmethod event-handler ::deploy-unit
  [{:keys [fx/context]}]
  (let [turn-order (subs/turn-order context)
        units (subs/units context)
        active (subs/active-id context)
        unit (subs/active-unit context)]
    (when (cu/deployed? unit)
      (mu/log ::unit-deployed
              :turn-order turn-order
              :unit unit
              :instrumentation :player)
      {:context (fx/swap-context context assoc
                                 :turn-order (rest turn-order)
                                 :units (assoc units active (cu/take-action unit))
                                 :active-unit nil)})))

(defmethod event-handler ::undeploy-unit
  [{:keys [fx/context]}]
  (let [units (subs/units context)
        active (subs/active-id context)
        unit (cu/undeploy (subs/active-unit context))]
    {:context (fx/swap-context context assoc :units (assoc units active unit))}))

;; Movement Phase

(defmethod event-handler ::turn-button-clicked
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :turn-flag true)})

(defmethod event-handler ::change-facing
  [{:keys [fx/context unit facing]}]
  (let [upd (cu/set-facing unit facing)
        units (assoc (subs/units context) (subs/active-id context) upd)]
    {:context (fx/swap-context context assoc :units units :turn-flag nil)}))

(defmethod event-handler ::set-movement-mode
  [{:keys [fx/context mode unit]}]
  (let [units (subs/units context)
        u (cu/set-movement-mode unit mode)
        upd (assoc units (:id u) u)]
    {:context (fx/swap-context context assoc :units upd)}))

(defmethod event-handler ::cancel-move
  [{:keys [fx/context unit]}]
  (let [active (subs/active-id context)
        upd (cu/cancel-movement unit)
        units (assoc (subs/units context) active upd)]
    {:context (fx/swap-context context assoc :units units)}))

(defmethod event-handler ::confirm-move
  [{:keys [fx/context unit]}]
  (let [turn-order (subs/turn-order context)
        units (subs/units context)
        active (subs/active-id context)
        upd (if (and (= (first turn-order) (cu/get-force unit)) (= active (cu/id unit)))
              (cu/move-unit unit (subs/board context))
              unit)]
    (if (cu/acted? upd)
      (do (mu/log ::move-confirmed
                  :unit upd
                  :destination (cu/get-location unit)
                  :remaining-moves (rest turn-order)
                  :instrumentation :player)
          {:context (fx/swap-context context assoc
                                     :turn-order (rest turn-order)
                                     :units (assoc units active upd)
                                     :turn-flag nil
                                     :active-unit nil)})
      (do (mu/log ::move-failed
                  :origin (cu/get-location unit)
                  :force (cu/get-force unit)
                  :force-conditional (= (first turn-order) (cu/get-force unit))
                  :active (cu/id unit)
                  :active-conditional (= active (cu/id unit))
                  :path (cu/get-path unit))
          {:context (fx/swap-context context assoc
                                     :turn-order turn-order
                                     :units units
                                     :turn-flag nil
                                     :active-unit active)}))))

;; Combat Phase
(defmethod event-handler ::set-attack
  [{:keys [fx/context targeting]}]
  (let [upd (cu/declare-special-attack (subs/active-unit context)
                                       targeting
                                       (subs/board context))
        units (assoc (subs/units context) (subs/active-id context) upd)]
    (mu/log ::SETTING-ATTACK
            :unit upd
            :selected targeting)
    {:context (fx/swap-context context assoc :units units :turn-order (rest (subs/turn-order context)))}))

(defmethod event-handler ::make-attack
  [{:keys [fx/context targeting]}]
  (let [attack-result (cu/make-attack targeting)
        units (merge (subs/units context) (:result attack-result))
        report (str (subs/round-report context) (reports/parse-attack-data attack-result))]
    {:context (fx/swap-context context assoc :units units :round-report report)}))

(defmethod event-handler ::close-attack-selection
  [{:keys [fx/context selected]}]
  (let [ctx (get-in context [:internal :attack-dialog])]
    (cond
      (= (:attack selected) nil)
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))}

      (contains? #{:charge :dfa} (:attack selected))
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))
       :dispatch {:event-type ::set-attack :targeting selected}}

      :else
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))
       :dispatch {:event-type ::make-attack :targeting selected}})))

(defmethod event-handler ::resolve-attacks
  [{:keys [fx/context]}]
  (let [layout (subs/layout context)
        board (subs/board context)]
    (loop [results {:units (subs/units context)
                    :round-report (subs/round-report context)}
           attackers (filter #(contains? #{:charge :dfa} (get % :atk-type false)) (subs/current-forces context))]
      (if (empty? attackers)
        (let [units (:units results)
              round-report (:round-report results)]
          {:context (fx/swap-context context assoc :units units :round-report round-report)})
        (recur (let [attacker (first attackers)
                     tgt-id (:target attacker)
                     target (get (:units results) tgt-id)
                     targeting (second (cu/->targeting attacker target board layout (:atk-type attacker)))
                     attack-result (cu/make-attack targeting)
                     units (merge (:units results) (:result attack-result))
                     report (str (:round-report results) (reports/parse-attack-data attack-result))]
                 (assoc results :units units :round-report report))
               (rest attackers))))))

(defmethod event-handler ::finish-attacks
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :turn-order (rest (subs/turn-order context)))})
