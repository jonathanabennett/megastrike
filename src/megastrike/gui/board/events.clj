(ns megastrike.gui.board.events
  (:require [cljfx.api :as fx]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]))

(defmethod events/event-handler ::hex-clicked
  [{:keys [fx/context hex]}]
   (let [phase (subs/phase context)
         active (fx/sub-val context :active-unit)
         units (fx/sub-val context :units)
         unit (get units active)
         next-force (first (subs/turn-order context))] 
     (cond 
       (and (= phase :deployment) (not (nil? unit)) (not (get unit :acted)) (= (get unit :force) next-force)) 
       (let [updated (merge unit (select-keys hex [:p :q :r])) 
             new-units (assoc units active updated)] 
         {:context (fx/swap-context context assoc :units new-units)})
       (and (= phase :movement) (not (nil? unit)) (not (get unit :acted)) (= (get unit :force) next-force)) 
       (when (cu/can-move? unit hex)
         (let [updated (merge unit (select-keys hex [:p :q :r]))
               ghosts (subs/unit-ghosts context)]
           {:context (fx/swap-context context assoc :ghosts (conj ghosts updated))})))))
