(ns megastrike.gui.board.events
  (:require [cljfx.api :as fx] 
            [com.brunobonacci.mulog :as mu]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex])
  (:import [javafx.scene.input MouseEvent]))

;; Add arctan(x2-x1 / y2-y1) formula to handle facing change.
(defmethod events/event-handler ::hex-clicked
  [{:keys [fx/context hex fx/event]}]
   (let [phase (subs/phase context)
         active (subs/active-id context)
         units (subs/units context)
         unit (subs/active-unit context) 
         next-force (first (subs/turn-order context))] 
     (cond 
       (and (some #(= phase %) [:deployment :movement]) (not (nil? unit)) (= (fx/sub-val context :turn-flag) true))
       (let [e ^MouseEvent event 
             u (if (:path unit) (last (:path unit)) unit)
             dest {:x (.getX e) :y (.getY e)}
             facing (hex/hex-facing u dest (subs/layout context))]
         {:dispatch {:event-type ::events/change-facing :unit unit :facing facing}})
       (and (= phase :deployment) (not (nil? unit)) (not (get unit :acted)) (= (get unit :force) next-force)) 
       (let [updated (merge unit (select-keys hex [:p :q :r])) 
             new-units (assoc units active updated)] 
         {:context (fx/swap-context context assoc :units new-units)})
       (and (= phase :movement) (not (nil? unit)) (not (get unit :acted)) (= (get unit :force) next-force)) 
       (let [updated (assoc unit :path (cu/find-path unit hex (subs/board context)))
             new-units (assoc units active updated)]
         {:context (fx/swap-context context assoc :units new-units)})
       (and (= phase :combat) (not (nil? unit)))
       (mu/log ::check-direction
               :unit unit
               :hex (hex/offset-from-hex hex)
               :facing (:direction unit)
               :rear (get-in cu/directions [(:direction unit) :rear])
               :is-behind (cu/detect-direction unit hex (get-in cu/directions [(:direction unit) :rear]) (fx/sub-val context :layout))
               ))))
