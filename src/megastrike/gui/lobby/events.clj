(ns megastrike.gui.lobby.events
  (:require [cljfx.api :as fx]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as e]
            [megastrike.gui.subs :as subs]
            [megastrike.phases :as phases]
            [megastrike.utils :as utils])
  (:import [javafx.event ActionEvent]
           [javafx.scene Node]
           [javafx.stage FileChooser]))

;; Make camo separate from color and, in the event of a camo being supplied, select a color from the camo.
(defmethod e/event-handler ::select-camo 
  [{:keys [^ActionEvent fx/context fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Camo")
                  (.setInitialDirectory (io/file "data/images/camo")))]
    (when-let [camo (.showOpenDialog chooser window)] 
      {:context (fx/swap-context context assoc :force-camo (str "file:" (.getPath camo)))})))

(defmethod e/event-handler ::load-mapboard
  [{:keys [^ActionEvent fx/context fx/event id]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Mapboard")
                  (.setInitialDirectory (io/file "data/boards")))
        boards (fx/sub-val context :map-boards)]
    (when-let [board (.showOpenDialog chooser window)]
      {:context (fx/swap-context context assoc :map-boards (assoc boards id (board/create-mapsheet (str "file:" (.getPath board)))))})))

(defmethod e/event-handler ::filter-changed
  [{:keys [fx/context field values]}]
  {:context (fx/swap-context context assoc :mul (cu/filter-units cu/mul field values))})

(defmethod e/event-handler ::color-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :force-color event)})

(defmethod e/event-handler ::launch-game
  [{:keys [fx/context view]}]
  (let [forces (phases/roll-initiative (subs/forces context)) 
        width (fx/sub-val context :width)
        height (fx/sub-val context :width)
        map-boards (fx/sub-val context :map-boards)
        turn-order (phases/generate-turn-order forces (vals (subs/units context)))]
    {:context (fx/swap-context context merge {:game-board (board/create-board map-boards width height)
                                              :forces forces 
                                              :turn-order turn-order 
                                              :current-phase :deployment 
                                              :display view})}))

(defmethod e/event-handler ::load-save
  [{:keys [fx/context]}]
   (let [save-data (edn/read-string (slurp (utils/load-resource :resources "save.edn")))]
     {:context (fx/swap-context context merge save-data)}))

(defmethod e/event-handler ::add-force
  [{:keys [fx/context]}]
  (let [name (fx/sub-val context :force-name)
        deploy (fx/sub-val context :force-zone)
        color (fx/sub-val context :force-color)
        camo (fx/sub-val context :force-camo)
        new-forces (merge (subs/forces context) {(utils/keyword-maker name) {:name name :deploy deploy :color color :camo camo}})] 
    {:context
     (fx/swap-context context assoc :forces new-forces :force-camo nil :force-color :white)}))

(defmethod e/event-handler ::mul-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-mul event)})

(defmethod e/event-handler ::add-unit
  [{:keys [fx/context]}]
  (let [units (fx/sub-val context :units)
        mul-unit (fx/sub-val context :active-mul)
        game-data {:force (fx/sub-val context :active-force) 
                   :pilot {:name (fx/sub-val context :pilot-name) 
                           :skill (Integer/parseInt (fx/sub-val context :pilot-skill))} 
                   :current-armor (:armor mul-unit) 
                   :current-structure (:structure mul-unit) 
                   :current-heat 0}]
    {:context (fx/swap-context context assoc :units (cu/create-element units mul-unit game-data))}))

(defmethod e/event-handler ::filter-mul
  [{:keys [fx/context field]}]
  (let [term (fx/sub-val context :mul-search-term)]
    {:context (fx/swap-context context assoc :mul (cu/filter-units cu/mul field term str/includes?))}))

(defmethod e/event-handler ::force-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-force (utils/keyword-maker (:name event)))})

(defmethod e/event-handler ::unit-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-unit (:id event))})
