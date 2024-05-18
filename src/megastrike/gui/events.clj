(ns megastrike.gui.events
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [megastrike.gui.subs :as subs]
            [megastrike.phases :as initiative]
            [megastrike.utils :as utils]))

(defmulti event-handler :event-type)

(defmethod event-handler :default [event]
  (prn event))

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::auto-save
  [{:keys [fx/context]}]
  (let [save {:game-board (fx/sub-val context :game-board)
              :units (subs/units context)
              :forces (fx/sub-val context :forces)}]
    (pprint/pprint save (io/writer "save.edn"))))

(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  (let [u (get (subs/units context) unit)]
    (when-not (:acted u) 
      {:context (fx/swap-context context assoc :active-unit unit)})))

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
   (let [phase (subs/phase context)
         active-force (first (subs/turn-order context))]
     (when (and (= phase :movement) (= active-force (:force unit)))
       {:context (fx/swap-context context assoc :active-unit (:id unit))})))

(defmethod event-handler ::roll-initiative
  [{:keys [fx/context]}]
  (let [forces (initiative/roll-initiative (fx/sub-val context :forces))
        turn-order (initiative/generate-turn-order forces (vals (subs/units context))) ]
    {:context (fx/swap-context context merge 
                               {:forces forces 
                                :turn-order turn-order 
                                :current-phase :deployment})}))

(defmethod event-handler ::next-phase 
  [{:keys [fx/context]}]
  (let [phase (subs/phase context)
        turn-number (subs/turn-number context)
        forces (fx/sub-val context :forces)
        units (subs/units context)]
    {:context (fx/swap-context context merge 
                               (initiative/next-phase {:current-phase phase 
                                                       :turn-number turn-number
                                                       :forces forces
                                                       :units units}))}))

(defmethod event-handler ::deploy-unit 
  [{:keys [fx/context]}]
  (let [turn-order (subs/turn-order context) 
        units (subs/units context)
        active (fx/sub-val context :active-unit)
        unit (merge (get units active) {:acted true})]
    {:context (fx/swap-context context assoc 
                               :turn-order (rest turn-order)
                               :units (assoc units active unit)
                               :active-unit nil)}))

(defmethod event-handler ::set-movement-mode
  [{:keys [fx/context mode unit]}]
  (let [units (subs/units context)
        u (merge unit {:movement-mode mode})
        upd (assoc units (:id u) u)]
    {:context (fx/swap-context context assoc :units upd)}))

(defmethod event-handler ::confirm-move
  [{:keys [fx/context unit]}]
  (let [turn-order (subs/turn-order context)
        units (subs/units context)
        active (fx/sub-val context :active-unit)
        ghost (some #(and (= (:id unit) (:id %)) %) (subs/unit-ghosts context))
        upd (assoc unit 
                   :p (:p ghost)
                   :q (:q ghost)
                   :r (:r ghost)
                   :acted true)] 
    (prn active)
    (prn (assoc units active upd))
    {:context (fx/swap-context context assoc
                               :turn-order (rest turn-order)
                               :units (assoc units active upd)
                               :ghosts (remove #(and (= (:id unit) (:id %)) %) (subs/unit-ghosts context))
                               :active-unit nil)}))