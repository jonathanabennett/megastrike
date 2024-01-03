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

(defmethod event-handler ::filter-changed
  [{:keys [fx/context field values]}]
  {:context (fx/swap-context context assoc :mul (cu/filter-membership cu/mul field values))})

(defmethod event-handler ::text-input
  [{:keys [fx/context key fx/event]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::color-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :force-color event)})

(defmethod event-handler ::add-force
  [{:keys [fx/context]}]
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
        matching-units (filter #(= (:full-name %) (:full-name mul-unit)) units)
        unit (merge mul-unit
                    {:force (fx/sub-val context :active-force)
                     :pilot {:name (fx/sub-val context :pilot-name)
                             :skill (Integer/parseInt (fx/sub-val context :pilot-skill))}
                     :current-armor (:armor mul-unit)
                     :current-structure (:structure mul-unit)
                     :current-heat 0
                     :id (if (seq matching-units)
                           (str (:full-name mul-unit) " #" (inc (count matching-units)))
                           (str (:full-name mul-unit)))})]
    {:context (fx/swap-context context assoc :units (conj (fx/sub-val context :units) unit))}))

(defmethod event-handler ::view-changed
  [{:keys [fx/context view]}]
  (let [new-board (board/create-board
                   (Integer/parseInt (fx/sub-val context :map-width))
                   (Integer/parseInt (fx/sub-val context :map-height)))]
    {:context (fx/swap-context context assoc :game-board new-board :display view)}))

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


(defmethod event-handler ::filter-mul
  [{:keys [fx/context field]}]
  (let [term (fx/sub-val context :mul-search-term)]
    {:context (fx/swap-context context assoc :mul (cu/filter-units cu/mul field term str/includes?))}))

(defmethod event-handler ::load-save
  [{:keys [fx/context]}]
   (let [save-data (edn/read-string (slurp "save.edn"))]
     {:context (fx/swap-context context merge context save-data {:display :game
                                                                 :mul []})}))

(defmethod event-handler ::stats-clicked
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-unit nil)})
