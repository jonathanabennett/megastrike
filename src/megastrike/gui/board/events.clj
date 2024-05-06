(ns megastrike.gui.board.events
  (:require [cljfx.api :as fx]
            [megastrike.gui.events :as events]))

(defmethod events/event-handler ::hex-clicked
  [{:keys [fx/context hex]}]
   (let [phase (fx/sub-val context :current-phase)
         active (fx/sub-val context :active-unit)
         units (fx/sub-val context :units)
         unit (get units active)
         next-force (first (fx/sub-val context :turn-order))]
     (when (and (= phase :deployment) (not (nil? unit)) (= (get unit :force) next-force)) 
       (let [updated (merge unit (select-keys hex [:p :q :r])) 
             new-units (assoc units active updated)] 
         {:context (fx/swap-context context assoc :units new-units)}))))
