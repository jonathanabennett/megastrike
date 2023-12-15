(ns megastrike.gui.events
  (:require
   [cljfx.api :as fx]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.utils :as utils]
   [megastrike.initiative :as initiative]
   [megastrike.gui.subs :as sub]))

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
     (fx/swap-context context assoc-in [:forces (utils/keyword-maker name)]
                      {:name name :deploy deploy :color color})}))

(defmethod event-handler ::force-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-force (utils/keyword-maker (:name event)))})

(defmethod event-handler ::mul-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-mul event)})

(defmethod event-handler ::unit-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-unit event)})

(defmethod event-handler ::add-unit
  [{:keys [fx/context]}]
  (let [units (fx/sub-val context :units)
        mul-unit (fx/sub-val context :active-mul)
        matching-units (filter #(= (:full-name %) (:name mul-unit)) units)
        unit (merge mul-unit
                    {:force (fx/sub-val context :active-force)
                     :pilot {:name (fx/sub-val context :pilot-name)
                             :skill (Integer/parseInt (fx/sub-val context :pilot-skill))}
                     :id (if (seq matching-units)
                           (str (:full-name mul-unit) " #" (inc (count matching-units)))
                           (str (:full-name mul-unit)))})]
    {:context (fx/swap-context context assoc :units (conj (fx/sub-val context :units) unit))}))

(defmethod event-handler ::view-changed
  [{:keys [fx/context view fx/event]}]
  {:context (fx/swap-context context assoc :display view)})
