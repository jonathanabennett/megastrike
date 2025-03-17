(ns megastrike.gui.lobby.events
  (:require
   [cljfx.api :as fx]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [megastrike.battle-force :as battle-force]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.events :as e]
   [megastrike.gui.subs :as subs]
   [megastrike.movement :as movement]
   [megastrike.mul :as mul]
   [megastrike.phases :as phases]
   [megastrike.scenario :as scenario]
   [megastrike.utils :as utils])
  (:import
   [javafx.event ActionEvent]
   [javafx.scene Node]
   [javafx.stage FileChooser]))

(defmethod e/event-handler ::select-camo
  [{:keys [^ActionEvent fx/context fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Camo")
                  (.setInitialDirectory (io/file "data/images/camo")))]
    (when-let [camo (.showOpenDialog chooser window)]
      {:context (fx/swap-context context assoc :force-camo (str "file:" (.getPath camo)))})))

(defmethod e/event-handler ::load-scenario
  [{:keys [^ActionEvent fx/context fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Scenario")
                  (.setInitialDirectory (io/file "data/scenarios")))]
    (when-let [s (.showOpenDialog chooser window)]
      (let [scenario (select-keys (scenario/setup-scenario s) [:units :forces :map-boards :map-width :map-height])]
        {:context (fx/swap-context context merge {:units nil :forces nil :map-boards nil :map-width "1" :map-height "1"} scenario)}))))

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
  {:context (fx/swap-context context assoc :mul (mul/filter-units mul/mul field values))})

(defmethod e/event-handler ::launch-game
  [{:keys [fx/context]}]
  (let [width (fx/sub-val context :width)
        height (fx/sub-val context :height)
        map-boards (fx/sub-val context :map-boards)
        response (phases/next-phase {:current-phase (subs/phase context)
                                     :turn-number (subs/turn-number context)
                                     :forces (subs/forces context)
                                     :units (subs/units context)})]
    {:context (fx/swap-context context merge
                               {:game-board (if (empty? map-boards)
                                              (subs/board context)
                                              (board/create-board map-boards width height))
                                :lobby false
                                :game true}
                               response)
     :dispatch {:event-type ::e/open-round-dialog}}))

(defmethod e/event-handler ::load-save
  [{:keys [fx/context]}]
  (let [save-data (edn/read-string
                   {:readers {'megastrike.movement.MechMovement movement/map->MechMovement
                              'megastrike.battle_force.BattleForce battle-force/map->BattleForce}} (slurp (utils/load-resource :data "save.edn")))]
    {:context (fx/swap-context context merge save-data)}))

(defmethod e/event-handler ::change-player
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :player event)})

(defmethod e/event-handler ::add-force
  [{:keys [fx/context]}]
  (let [force-name (fx/sub-val context :force-name)
        deploy (fx/sub-val context :force-zone)
        camo (fx/sub-val context :force-camo)
        team (inc (count (subs/forces context)))
        player (fx/sub-val context :player)
        new-force (battle-force/create-force force-name deploy camo team player)
        new-forces (merge (subs/forces context) {(battle-force/id new-force) new-force})]
    {:context (fx/swap-context context assoc :forces new-forces :force-camo nil)
     :dispatch {:event-type ::e/close-dialog :dialog :force-creation-dialog}}))

(defmethod e/event-handler ::mul-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-mul event)})

(defmethod e/event-handler ::add-unit
  [{:keys [fx/context]}]
  (let [units (subs/units context)
        mul-unit (fx/sub-val context :active-mul)
        pilot {:name (fx/sub-val context :pilot-name)
               :skill (Integer/parseInt (fx/sub-val context :pilot-skill))}
        battle-force (fx/sub-val context :active-force)]
    {:context (fx/swap-context context assoc :units (cu/->element units mul-unit pilot battle-force))
     :dispatch {:event-type ::e/close-dialog :dialog :mul-dialog}}))

(defmethod e/event-handler ::filter-mul
  [{:keys [fx/context field]}]
  (let [term (fx/sub-val context :mul-search-term)]
    {:context (fx/swap-context context assoc :mul (mul/filter-units mul/mul field term str/includes?))}))

(defmethod e/event-handler ::force-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc
                             :active-force (battle-force/id event)
                             :active-force-record event
                             :force-name (battle-force/to-str event)
                             :force-zone (battle-force/get-deployment event)
                             :force-camo (battle-force/get-camo event))})

(defmethod e/event-handler ::unit-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :active-unit (:id event))})
