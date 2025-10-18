(ns megastrike.gui.events
  (:require
   [cljfx.api :as fx]
   [com.brunobonacci.mulog :as mu]
   [megastrike.gui.subs :as subs]
   [megastrike.logs :as logs]
   [megastrike.turn-manager :as turn-manager]
   [megastrike.scenario :as scenario])
  (:import
   [javafx.application Platform]
   [javafx.scene.control ButtonBar$ButtonData ButtonType Dialog DialogEvent]
   [javafx.scene.input MouseEvent]))

;; Defaults and common operations
(defmulti event-handler :event-type)

(defmethod event-handler ::show-confirmation
  [{:keys [fx/context dialog-id]}]
  {:context (fx/swap-context context assoc-in [:gui :dialogs dialog-id :showing] true)})

(defmethod event-handler ::on-confirmation-dialog-hidden
  [{:keys [fx/context ^DialogEvent fx/event dialog-id on-confirmed]}]
  (condp = (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))
    ButtonBar$ButtonData/CANCEL_CLOSE
    {:context (fx/swap-context context assoc-in [:gui :dialogs dialog-id :showing] false)}

    ButtonBar$ButtonData/OK_DONE
    {:context (fx/swap-context context assoc-in [:gui :dialogs dialog-id :showing] false)
     :dispatch on-confirmed}))

(defmethod event-handler ::close-dialog
  [{:keys [fx/context dialog]}]
  {:context (fx/swap-context context assoc-in [:gui :dialogs dialog :showing] false)})

(defmethod event-handler ::no-op
  [_])

(defmethod event-handler :default
  [{:keys [event-type] :as event}]
  (mu/log ::unhandled-event
          :event-type event-type
          :keys (keys event)))

;; board events
(defmethod event-handler ::hex-clicked
  [{:keys [fx/context hex fx/event]}]
  (let [e ^MouseEvent event
        click-location {:x (.getX e) :y (.getY e)}
        active-unit (subs/active-id context)]
    {:context (fx/swap-context context assoc :game
                               (turn-manager/hex-clicked (subs/game context)
                                                         (subs/layout context)
                                                         hex click-location active-unit))}))

(defmethod event-handler ::text-input
  [{:keys [fx/context ks fx/event]}]
  {:context (fx/swap-context context assoc-in ks event)})

(defmethod event-handler ::change-size
  [{:keys [fx/context direction]}]
  (let [layout (subs/layout context)
        new-layout (if (= direction :plus)
                     (assoc layout :scale (+ (:scale layout) 0.1))
                     (assoc layout :scale (- (:scale layout) 0.1)))]
    {:context (fx/swap-context context assoc-in [:gui :layout] new-layout)}))

;; Saving, loading, and Phases
(defmethod event-handler ::auto-save
  [{:keys [fx/context]}]
  (prn (keys (subs/game context)))
  (spit "test-data.edn" (scenario/edn-scenario-writer (subs/game context))))

(defmethod event-handler ::quit-game
  [_]
  (logs/logs)
  (Platform/exit))

(defmethod event-handler ::open-round-dialog
  [{:keys [fx/context]}]
  (let [ctx (get-in context [:dialogs :round-dialog])
        advance-phase? (empty? (subs/turn-order context))]
    {:context (fx/swap-context context assoc-in [:dialogs :round-dialog]
                               (assoc ctx :showing true :advance-phase? advance-phase?))}))

(defmethod event-handler ::close-round-dialog
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc-in [:dialogs :round-dialog]
                             {:showing false :advance-phase? false})})

(defmethod event-handler ::next-phase
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/advance-turn (subs/game context)
                                                        (subs/gui context)
                                                        (subs/layout context)))
   :dispatch {:event-type ::open-round-dialog}})

;; Unit selection
(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context assoc :gui
                             (turn-manager/switch-unit (subs/game context) (subs/gui context) unit))})

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
  (let [old-state (subs/game context)
        old-gui (subs/gui context)
        response (turn-manager/unit-clicked old-state old-gui unit)
        new-state (:game response)
        new-gui (:gui response)]
    {:context (fx/swap-context context assoc :game new-state :gui new-gui)}))

;; Deployment Phase
(defmethod event-handler ::deploy-unit
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/deploy-unit (subs/game context)
                                                       (subs/gui context)))})

(defmethod event-handler ::undeploy-unit
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/undeploy-unit (subs/game context)
                                                         (subs/gui context)))})

;; Movement Phase

(defmethod event-handler ::turn-button-clicked
  [{:keys [fx/context]}]
  {:context (fx/swap-context context update-in [:game :turn-flag] not)})

(defmethod event-handler ::set-movement-mode
  [{:keys [fx/context unit mode]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/set-movement-mode (subs/game context) unit mode))})

(defmethod event-handler ::cancel-move
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/cancel-move (subs/game context) unit))})

(defmethod event-handler ::confirm-move
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/confirm-move (subs/game context)
                                                        (subs/gui context)
                                                        (subs/layout context)))})

;; Combat Phase
(defmethod event-handler ::set-attack
  [{:keys [fx/context targeting]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/set-special-attack (subs/game context)
                                                              (subs/gui context)
                                                              targeting))
   :dispatch {:event-type ::next-phase}})

(defmethod event-handler ::finish-attacks
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/advance-turn (subs/game context)
                                                        (subs/gui context)
                                                        (subs/layout context)))})

(defmethod event-handler ::close-attack-selection
  [{:keys [fx/context selected]}]
  (let [ctx (get-in context [:gui :dialogs :attack-dialog])]
    (cond
      (= (:targeting/attack-type selected) nil)
      {:context (fx/swap-context context assoc-in [:gui :dialogs :attack-dialog]
                                 (assoc ctx :showing false :items []))}

      (contains? #{:attack/charge :attack/dfa} (:attack selected))
      {:context (fx/swap-context context assoc-in [:gui :dialogs :attack-dialog]
                                 (assoc ctx :showing false :items []))
       :dispatch {:event-type ::set-attack :targeting selected}}

      :else
      {:context (fx/swap-context context assoc-in [:gui :dialogs :attack-dialog]
                                 (assoc ctx :showing false :items []))
       :dispatch {:event-type ::make-attack :targeting selected}})))

(defmethod event-handler ::make-attack
  [{:keys [fx/context targeting]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/make-attack (subs/game context) targeting))
   :dispatch {:event-type ::open-round-dialog}})

(defmethod event-handler ::resolve-physicals
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :game
                             (turn-manager/resolve-physical-attacks (subs/game context) (subs/layout context)))
   :dispatch {:event-type ::open-round-dialog}})

