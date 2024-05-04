(ns megastrike.gui.events
  (:require
   [clojure.edn :as edn]
   [cljfx.api :as fx]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.utils :as utils]
   [megastrike.initiative :as initiative]
   [megastrike.gui.subs :as sub]
   [clojure.string :as str]))

(defmulti event-handler :event-type)

(defmethod event-handler :default [event]
  (prn event))

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::force-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-force (utils/keyword-maker (:name event)))})

(defmethod event-handler ::unit-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-unit (:id event))})

(defmethod event-handler ::auto-save
  [{:keys [fx/context]}]
  (let [save {:game-board (fx/sub-val context :game-board)
              :units (fx/sub-val context :units)
              :forces (fx/sub-val context :forces)}]
    (spit "save.edn" save)))

;; (defmethod event-handler ::initiative-phase
;;   [{:keys [fx/context fx/event]}]
;;   (let [initiatives (sort-by :roll (initiative/roll-initiative))
;;         units (sub/units-by-force context)]
;;      ))

(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  {:context (fx/swap-context context assoc :active-unit unit)})

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
   (prn unit))

(defmethod event-handler ::roll-initiative
  [{:keys [fx/context]}]
  (let [forces (initiative/roll-initiative (fx/sub-val context :forces))
        turn-order (initiative/generate-turn-order forces (sub/units context))
        phase :deployment
        turn (fx/sub-val context :turn-number) ]
    {:context (fx/swap-context context merge {:forces forces 
                                              :turn-order turn-order 
                                              :current-phase phase })}))
