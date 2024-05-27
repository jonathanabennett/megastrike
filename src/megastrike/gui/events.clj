(ns megastrike.gui.events
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [megastrike.gui.subs :as subs]
            [megastrike.phases :as initiative]
            [megastrike.utils :as utils]
            [megastrike.combat-unit :as cu]))

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
    (pprint/pprint save (io/writer (utils/load-resource "save.edn")))))

(defmethod event-handler ::stats-clicked
  [{:keys [fx/context unit]}]
  (let [u (get (subs/units context) unit)]
    (when-not (:acted u) 
      {:context (fx/swap-context context assoc :active-unit unit)})))

(defmethod event-handler ::unit-clicked
  [{:keys [fx/context unit]}]
   (let [phase (subs/phase context)
         active-force (first (subs/turn-order context))
         active-unit (get (subs/units context) (fx/sub-val context :active-unit))]
     (when (and (= active-force (:force unit)) 
                (not (:acted unit)))
       {:context (fx/swap-context context assoc :active-unit (:id unit))})
     (when (and (= phase :combat) 
                (not (= active-force (:force unit))))
       (let [upd (assoc (subs/units context) (:id active-unit) (assoc active-unit :target (:id unit)))]
         {:context (fx/swap-context context assoc :units upd)}))))

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

(defmethod event-handler ::undeploy-unit
  [{:keys [fx/context]}]
  (let [units (subs/units context)
        active (fx/sub-val context :active-unit)
        unit (merge (get units active) {:p nil :q nil :r nil})]
    {:context (fx/swap-context context assoc :units (assoc units active unit))}))

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
        upd (merge unit 
                   (when ghost 
                     {:p (:p ghost) 
                      :q (:q ghost) 
                      :r (:r ghost)})
                   {:acted true})] 
    {:context (fx/swap-context context assoc
                               :turn-order (rest turn-order)
                               :units (assoc units active upd)
                               :ghosts (remove #(and (= (:id unit) (:id %)) %) (subs/unit-ghosts context))
                               :active-unit nil)}))

(defmethod event-handler ::make-attacks 
  [{:keys [fx/context]}]
    (loop [units (subs/units context)
           attackers (filter #(:target %) (subs/current-forces context))]
      (if (empty? attackers)
        {:context (fx/swap-context context assoc
                                   :units units 
                                   :active-unit nil
                                   :turn-order (rest (subs/turn-order context)))}
        (let [attacker (first attackers)
              target (get units (:target attacker))
              upd (cu/make-attack attacker target)]
          (recur (assoc units (:id target) upd)
                 (rest attackers))))))