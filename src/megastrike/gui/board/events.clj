(ns megastrike.gui.board.events
  (:require
   [cljfx.api :as fx]
   [com.brunobonacci.mulog :as mu]
   [megastrike.gui.events :as events]
   [megastrike.gui.subs :as subs]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement])
  (:import
   [javafx.scene.input MouseEvent]))

(defn- facing-change [unit event layout]
  (let [e ^MouseEvent event
        u (if (:path unit) (last (:path unit)) unit)
        dest {:x (.getX e) :y (.getY e)}
        facing (hex/facing u dest layout)]
    {:dispatch {:event-type ::events/change-facing :unit unit :facing facing}}))

(defmethod events/event-handler ::hex-clicked
  [{:keys [fx/context hex fx/event]}]
  (let [phase (subs/phase context)
        active (subs/active-id context)
        units (subs/units context)
        unit (subs/active-unit context)
        next-force (first (subs/turn-order context))]
    (if unit
      (cond
        (and (= phase :deployment) (not (get unit :acted)) (= (get unit :force) next-force))
        (let [updated (merge unit (select-keys hex [:p :q :r]))
              new-units (assoc units active updated)]
          {:context (fx/swap-context context assoc :units new-units)})
        (and (some #{phase} [:deployment :movement]) (not (nil? unit)) (fx/sub-val context :turn-flag))
        (facing-change unit event (subs/layout context))
        (and (= phase :movement) (not (get unit :acted)) (= (get unit :force) next-force))
        (let [updated (assoc unit :path (movement/find-path unit hex (subs/board context)))
              new-units (assoc units active updated)]
          {:context (fx/swap-context context assoc :units new-units)}))
      (mu/log ::no-active-unit))))
