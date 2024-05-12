(ns megastrike.gui.lobby.events
  (:require [cljfx.api :as fx]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as e]
            [megastrike.gui.subs :as sub]
            [megastrike.phases :as phases]
            [megastrike.utils :as utils]))

(defmethod e/event-handler ::filter-changed
  [{:keys [fx/context field values]}]
  {:context (fx/swap-context context assoc :mul (cu/filter-membership cu/mul field values))})

(defmethod e/event-handler ::color-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :force-color event)})

(defmethod e/event-handler ::launch-game
  [{:keys [fx/context view]}]
  (let [new-board (board/create-board
                   (Integer/parseInt (fx/sub-val context :map-width))
                   (Integer/parseInt (fx/sub-val context :map-height)))
        forces (phases/roll-initiative (fx/sub-val context :forces))
        turn-order (phases/generate-turn-order forces (vals (sub/units context)))]
    {:context (fx/swap-context context merge {:game-board new-board 
                                              :forces forces 
                                              :turn-order turn-order 
                                              :current-phase :deployment 
                                              :display view})}))

(defmethod e/event-handler ::load-save
  [{:keys [fx/context]}]
   (let [save-data (edn/read-string (slurp "save.edn"))]
     {:context (fx/swap-context context merge save-data)}))

(defmethod e/event-handler ::add-force
  [{:keys [fx/context]}]
  (let [name (fx/sub-val context :force-name)
        deploy (fx/sub-val context :force-zone)
        color (fx/sub-val context :force-color)]
    {:context
     (fx/swap-context context assoc-in [:forces (utils/keyword-maker name)]
                      {:name name :deploy deploy :color color})}))

(defmethod e/event-handler ::mul-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-mul event)})

(defmethod e/event-handler ::add-unit
  [{:keys [fx/context]}]
  (let [units (fx/sub-val context :units)
        mul-unit (fx/sub-val context :active-mul)
        matching-units (filter #(= (:full-name %) (:full-name mul-unit)) units)
        id (if (seq matching-units)
                           (str (:full-name mul-unit) " #" (inc (count matching-units)))
                           (str (:full-name mul-unit)))
        unit (merge mul-unit
                    {:force (fx/sub-val context :active-force)
                     :pilot {:name (fx/sub-val context :pilot-name)
                             :skill (Integer/parseInt (fx/sub-val context :pilot-skill))}
                     :current-armor (:armor mul-unit)
                     :current-structure (:structure mul-unit)
                     :current-heat 0
                     :id id})]
    {:context (fx/swap-context context assoc :units (assoc (fx/sub-val context :units) id unit))}))

(defmethod e/event-handler ::filter-mul
  [{:keys [fx/context field]}]
  (let [term (fx/sub-val context :mul-search-term)]
    {:context (fx/swap-context context assoc :mul (cu/filter-units cu/mul field term str/includes?))}))
