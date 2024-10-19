(ns megastrike.gui.events
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [com.brunobonacci.mulog :as mu]
            [megastrike.attacks :as attacks]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex]
            [megastrike.movement :as movement]
            [megastrike.phases :as initiative]
            [megastrike.gui.reports :as reports]
            [megastrike.logs :as logs]
            [megastrike.utils :as utils]
            [megastrike.combat-unit :as cu])
  (:import [javafx.application Platform]
           [javafx.scene.control
            ButtonBar$ButtonData
            ButtonType
            Dialog
            DialogEvent]))

;; Defaults and common operations
(defmulti event-handler :event-type)

(defmethod event-handler ::no-op
  [_])

(defmethod event-handler :default [{:keys [event-type] :as event}]
  (prn "EVENT!")
  (mu/log ::unhandled-event
          :event-type event-type
          :keys (keys event)))

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::show-popup
  [{:keys [fx/context state-id]}]
  (mu/log ::showing-popup
          :popup state-id)
  {:context (fx/swap-context context assoc-in [:internal state-id :showing] true)})

(defmethod event-handler ::hide-popup
  [{:keys [fx/context ^DialogEvent fx/event state-id on-confirmed]}]
  (mu/log ::hiding-popup
          :popup state-id)
  (condp = (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))
    ButtonBar$ButtonData/OK_DONE
    {:context (fx/swap-context context assoc-in [:internal state-id :showing] false)
     :dispatch on-confirmed}
    ButtonBar$ButtonData/CANCEL_CLOSE
    {:context (fx/swap-context context assoc-in [:internal state-id :showing] false)}))

(defmethod event-handler ::change-size
  [{:keys [fx/context direction]}]
  (when (= (fx/sub-val context :display) :game)
    (let [layout (fx/sub-val context :layout)
          new-layout (if (= direction :plus)
                       (assoc layout :scale (+ (:scale layout) 0.1))
                       (assoc layout :scale (- (:scale layout) 0.1)))]
      {:context (fx/swap-context context assoc :layout new-layout)})))

;; Saving, loading, and Phases
(defmethod event-handler ::auto-save
  [{:keys [fx/context]}]
  (let [save {:game-board (fx/sub-val context :game-board)
              :units (subs/units context)
              :forces (fx/sub-val context :forces)}]
    (mu/log ::auto-save-event)
    (pprint/pprint save (io/writer (utils/load-resource :data "save.edn")))))

(defmethod event-handler ::quit-game
  [_]
  (logs/logs)
  (Platform/exit))

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
                                         :round-report (fx/sub-val context :round-report)})]
    {:context (fx/swap-context context merge response)
     :dispatch {:event-type ::show-popup :state-id :round-dialog}}))

;; Unit selection
(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  (let [u (get (subs/units context) unit)]
    (when (and (= (:force u) (first (subs/turn-order context))) (not (:acted u)))
      (mu/log ::stats-clicked-event :clicked unit)
      {:context (fx/swap-context context assoc :active-unit unit)})))

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
  (let [phase (subs/phase context)
        active-force (first (subs/turn-order context))
        active-unit (subs/active-unit context)
        board (subs/board context)
        layout (subs/layout context)]
    (mu/with-context {:unit-clicked unit :phase phase}
      (cond
        (and (= active-force (:force unit)) (not (:acted unit)))
        (do
          (mu/log ::select-unit)
          {:context (fx/swap-context context assoc :active-unit (:id unit))})
        (and (= phase :movement) (not (= active-force (:force unit))))
        (let [can-charge? (cu/can-charge? active-unit unit)
              can-dfa? (and (= (:movement-mode unit) :jump) (cu/can-charge? active-unit unit))
              kind (cond
                     can-charge? :charge
                     can-dfa? :dfa
                     :else :none)
              attacks (attacks/physical-confirmation-choices active-unit unit board layout kind)
              ctx (get-in context [:internal :attack-dialog])]
          (mu/log ::attempted-charge
                  :active (:full-name active-unit)
                  :target (:full-name unit)
                  :can-charge? can-charge?
                  :can-dfa? can-dfa?
                  :attacks attacks)
          (when (not= kind :none)
            {:context (fx/swap-context context assoc-in [:internal :attack-dialog]
                                       (assoc ctx :showing true :items attacks :unit unit))}))
        (and (= phase :combat) (not (= active-force (:force unit))))
        (let [ctx (get-in context [:internal :attack-dialog])]
          {:context (fx/swap-context context assoc-in [:internal :attack-dialog]
                                     (assoc ctx :showing true
                                            :items (attacks/attack-confirmation-choices active-unit unit board layout)
                                            :unit unit))})))))

;; Initiative Phase
(defmethod event-handler ::roll-initiative
  [{:keys [fx/context]}]
  (let [forces (initiative/roll-initiative (fx/sub-val context :forces))
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
    (when (:q unit)
      (mu/log ::unit-deployed
              :turn-order turn-order
              :unit unit
              :instrumentation :player)
      {:context (fx/swap-context context assoc
                                 :turn-order (rest turn-order)
                                 :units (assoc units active (merge unit {:acted true}))
                                 :active-unit nil)})))

(defmethod event-handler ::undeploy-unit
  [{:keys [fx/context]}]
  (let [units (subs/units context)
        active (subs/active-id context)
        unit (merge (subs/active-unit context) {:p nil :q nil :r nil})]
    {:context (fx/swap-context context assoc :units (assoc units active unit))}))

;; Movement Phase

(defmethod event-handler ::turn-button-clicked
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :turn-flag true)})

(defmethod event-handler ::change-facing
  [{:keys [fx/context unit facing]}]
  (let [upd (merge unit {:direction facing})
        units (assoc (subs/units context) (:full-name upd) upd)]
    {:context (fx/swap-context context assoc :units units :turn-flag nil)}))

(defmethod event-handler ::set-movement-mode
  [{:keys [fx/context mode unit]}]
  (let [units (subs/units context)
        u (merge unit {:movement-mode mode})
        upd (assoc units (:id u) u)]
    {:context (fx/swap-context context assoc :units upd)}))

(defmethod event-handler ::cancel-move
  [{:keys [fx/context unit]}]
  (let [active (subs/active-id context)
        upd (assoc unit :path [])
        units (assoc (subs/units context) active upd)]
    {:context (fx/swap-context context assoc :units units)}))

(defmethod event-handler ::confirm-move
  [{:keys [fx/context unit]}]
  (let [turn-order (subs/turn-order context)
        units (subs/units context)
        active (subs/active-id context)
        upd (when (and (= (first turn-order) (:force unit)) (= active (:id unit)))
              (movement/move-unit unit (subs/board context)))]
    (when (:acted upd)
      (mu/log ::move-confirmed
              :unit upd
              :destination (hex/hex->offset (select-keys upd [:p :q :r]))
              :remaining-moves (rest turn-order)
              :instrumentation :player)
      {:context (fx/swap-context context assoc
                                 :turn-order (rest turn-order)
                                 :units (assoc units active upd)
                                 :turn-flag nil
                                 :active-unit nil)})))

;; Combat Phase
(defmethod event-handler ::set-attack
  [{:keys [fx/context unit selected]}]
  (let [active-id (subs/active-id context)
        active (subs/active-unit context)
        upd (assoc active :target (:id unit) :attack selected)
        units (assoc (subs/units context) active-id upd)]
    (mu/log ::set-attack-event
            :attacker upd
            :target (:id unit)
            :attack-type selected
            :instrumentation :player)
    {:context (fx/swap-context context assoc :units units)}))

(defmethod event-handler ::make-attack
  [{:keys [fx/context unit selected]}]
  (let [active (subs/active-unit context)
        upd (assoc active :target (:id unit) :attack selected :acted true)
        data (attacks/make-attack upd unit selected)
        units (merge (subs/units context) (:result data))
        report (str (fx/sub-val context :round-report) (reports/parse-attack-data data))]
    {:context (fx/swap-context context assoc :units units :round-report report)}))

(defmethod event-handler ::close-attack-selection
  [{:keys [fx/context unit selected]}]
  (let [ctx (get-in context [:internal :attack-dialog])]
    (cond
      (= (:flag selected) nil)
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))}
      (contains? #{:regular :physical} (:flag selected))
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))
       :dispatch {:event-type ::make-attack :unit unit :selected selected}}
      (contains? #{:charge :dfa} (:flag selected))
      {:context (fx/swap-context context assoc-in [:internal :attack-dialog] (assoc ctx :showing false :items []))
       :dispatch {:event-type ::set-attack :unit unit :selected selected}})))

(defmethod event-handler ::resolve-attacks
  [{:keys [fx/context]}]
  (loop [results {:units (subs/units context)
                  :round-report (fx/sub-val context :round-report)}
         attackers (filter #(contains? #{:charge :dfa} (get-in % [:attack :flag]))
                           (subs/current-forces context))]
    (if (empty? attackers)
      {:context (fx/swap-context context merge results)}
      (recur (let [attacker (merge (first attackers) {:acted true})
                   target (get (subs/units context) (get-in attacker [:attack :target]))
                   attack-result (attacks/make-attack attacker target (:attack attacker))
                   units (merge (:units results) (:results attack-result))
                   report (str (:round-report results) (reports/parse-attack-data attack-result))]
               (assoc results :units units :round-report report))
             (rest attackers)))))

(defmethod event-handler ::finish-attacks
  [{:keys [fx/context]}]
  {:context (fx/swap-context context assoc :turn-order (rest (subs/turn-order context)))})
