(ns megastrike.gui.events
  (:require
   [cljfx.api :as fx]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [com.brunobonacci.mulog :as mu]
   [megastrike.gui.subs :as subs]
   [megastrike.logs :as logs]
   [megastrike.turn-manager :as turn-manager]
   [megastrike.utils :as utils])
  (:import
   [javafx.application Platform]
   [javafx.scene.control ButtonBar$ButtonData ButtonType Dialog DialogEvent]
   [javafx.scene.input MouseEvent]))

;; Defaults and common operations
(defmulti event-handler :event-type)

(defmethod event-handler ::show-confirmation
  [{:keys [fx/context dialog-id]}]
  {:context (fx/swap-context context assoc-in [:internal dialog-id :showing] true)})

(defmethod event-handler ::on-confirmation-dialog-hidden
  [{:keys [fx/context ^DialogEvent fx/event dialog-id on-confirmed]}]
  (condp = (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))
    ButtonBar$ButtonData/CANCEL_CLOSE
    {:context (fx/swap-context context assoc-in [:internal dialog-id :showing] false)}

    ButtonBar$ButtonData/OK_DONE
    {:context (fx/swap-context context assoc-in [:internal dialog-id :showing] false)
     :dispatch on-confirmed}))

(defmethod event-handler ::close-dialog
  [{:keys [fx/context dialog]}]
  {:context (fx/swap-context context assoc-in [:internal dialog :showing] false)})

(defmethod event-handler ::no-op
  [_])

(defmethod event-handler :default
  [{:keys [event-type] :as event}]
  (prn "EVENT!")
  (mu/log ::unhandled-event
          :event-type event-type
          :keys (keys event)))

;; board events
(defmethod event-handler ::hex-clicked
  [{:keys [fx/context hex fx/event]}]
  (let [e ^MouseEvent event
        click-location {:x (.getX e) :y (.getY e)}]
    {:context (fx/swap-context context turn-manager/hex-clicked hex click-location)}))

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
  {:context (fx/swap-context context assoc :round-dialog {:showing false :advance-phase? false})})

(defmethod event-handler ::next-phase
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/advance-turn)
   :dispatch {:event-type ::open-round-dialog}})

;; Unit selection
(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context turn-manager/switch-unit unit)})

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context turn-manager/unit-clicked unit)})

;; Deployment Phase
(defmethod event-handler ::deploy-unit
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/deploy-unit)})

(defmethod event-handler ::undeploy-unit
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/undeploy-unit)})

;; Movement Phase

(defmethod event-handler ::turn-button-clicked
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :turn-flag true)})

(defmethod event-handler ::set-movement-mode
  [{:keys [fx/context unit mode]}]
  {:context (fx/swap-context context turn-manager/set-movement-mode unit mode)})

(defmethod event-handler ::cancel-move
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context turn-manager/cancel-move unit)})

(defmethod event-handler ::confirm-move
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/confirm-move)})

;; Combat Phase
(defmethod event-handler ::set-attack
  [{:keys [fx/context targeting]}]
  {:context (fx/swap-context context turn-manager/set-special-attack targeting)
   :dispatch {:event-type ::next-phase}})

(defmethod event-handler ::finish-attacks
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/advance-turn)})

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

(defmethod event-handler ::make-attack
  [{:keys [fx/context targeting]}]
  {:context (fx/swap-context context turn-manager/make-attack targeting)
   :dispatch {:event-type ::open-round-dialog}})

(defmethod event-handler ::resolve-physicals
  [{:keys [fx/context]}]
  {:context (fx/swap-context context turn-manager/resolve-physical-attacks)
   :dispatch {:event-type ::open-round-dialog}})

