(ns megastrike.gui.events
  (:require
   [cljfx.api :as fx]
   [megastrike.combat-unit :as cu]))

(defmulti event-handler :event-type)

(defmethod event-handler :default [event]
  (prn event))

(defmethod event-handler ::filter-changed
  [{:keys [fx/context field values fx/event]}]
  {:context (fx/swap-context context assoc :mul (cu/filter-membership cu/mul field values))})

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::color-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :force-color event)})

(defmethod event-handler ::add-force
  [{:keys [fx/context fx/event]}]
  (let [name (fx/sub-val context :force-name)
        deploy (fx/sub-val context :force-zone)
        color (fx/sub-val context :force-color)]
    {:context
     (fx/swap-context context assoc-in [:forces name]
                      {:name name :deploy deploy :color color})}))
