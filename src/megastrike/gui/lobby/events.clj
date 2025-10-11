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
   [megastrike.phases :as phases]
   [megastrike.scenario :as scenario]
   [megastrike.utils :as utils])
  (:import
   [javafx.event ActionEvent]
   [javafx.scene Node]
   [javafx.stage FileChooser]))

(def empty-game
  {:game {:units nil :forces nil :current-phase :lobby :map-width "1" :map-height "1" :turn-number 0}})

(defmethod e/event-handler ::select-camo
  [{:keys [^ActionEvent fx/context fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Camo")
                  (.setInitialDirectory (io/file "data/images/camo")))]
    (when-let [camo (.showOpenDialog chooser window)]
      {:context (fx/swap-context context assoc-in [:lobby :force-camo] (str "file:" (.getPath camo)))})))

(defmethod e/event-handler ::load-scenario
  [{:keys [^ActionEvent fx/context fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Scenario")
                  (.setInitialDirectory (io/file "data/scenarios")))]
    (when-let [s (.showOpenDialog chooser window)]
      (let [response (scenario/setup-scenario s)
            lobby (-> (subs/lobby context)
                      (merge (:lobby response)))
            game (-> (subs/game context)
                     (merge empty-game (:game response)))]
        {:context (fx/swap-context context assoc :game game :lobby lobby)}))))

(defmethod e/event-handler ::load-mapboard
  [{:keys [^ActionEvent fx/context fx/event id]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Select Mapboard")
                  (.setInitialDirectory (io/file "data/boards")))
        boards (subs/board context)]
    (when-let [board (.showOpenDialog chooser window)]
      {:context (fx/swap-context context assoc-in [:lobby :map-boards] (assoc boards id (board/create-mapsheet (str "file:" (.getPath board)))))})))

(defmethod e/event-handler ::filter-changed
  [{:keys [fx/context values]}]
  {:context (fx/swap-context context assoc-in [:lobby :mul] (cu/filter-units cu/mul values))})

(defmethod e/event-handler ::launch-game
  [{:keys [fx/context]}]
  (let [width (subs/map-width context)
        height (subs/map-height context)
        map-boards (if (empty? (subs/map-boards context))
                     (subs/board context)
                     (board/create-board (subs/map-boards context) width height))
        game (-> (subs/game context)
                 (assoc :game-board map-boards)
                 (phases/next-phase))
        gui (-> (subs/gui context)
                (assoc :game-view true)
                (assoc :lobby-view false))]
    {:context (fx/swap-context context assoc
                               :game game :gui gui)
     :dispatch {:event-type ::e/open-round-dialog}}))

(defmethod e/event-handler ::load-save
  [{:keys [fx/context]}]
  (let [save-data (edn/read-string
                   (slurp (utils/load-resource :data "save.edn")))]
    {:context (fx/swap-context context merge save-data)}))

(defmethod e/event-handler ::change-player
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [:lobby :player] event)})

(defmethod e/event-handler ::add-force
  [{:keys [fx/context]}]
  (let [fname (subs/force-name context)
        deploy (subs/force-zone context)
        camo (subs/force-camo context)
        team (inc (count (subs/forces context)))
        player (subs/player-type context)
        new-force (battle-force/->battle-force fname deploy camo team player [])
        new-forces (battle-force/update-battle-force (subs/forces context) (:unit-group/keyword new-force) new-force)
        new-game (merge (subs/game context) {:forces new-forces})
        new-lobby (merge (subs/lobby context) {:force-camo nil})]
    {:context (fx/swap-context context assoc :game new-game :lobby new-lobby)
     :dispatch {:event-type ::e/close-dialog :dialog :force-creation-dialog}}))

(defmethod e/event-handler ::mul-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [:lobby :active-mul] event)})

(defmethod e/event-handler ::add-unit
  [{:keys [fx/context]}]
  (let [units (subs/units context)
        mul-unit (subs/active-mul context)
        pilot {:name (subs/p-name context)
               :skill (subs/p-skill context)}
        battle-force (subs/lobby-active-force context)]
    {:context
     (fx/swap-context context assoc-in [:game :units]
                      (cu/->combat-unit
                       {:units units
                        :mul-unit mul-unit
                        :pilot pilot
                        :battle-force battle-force}))
     :dispatch {:event-type ::e/close-dialog :dialog :mul-dialog}}))

(defmethod e/event-handler ::filter-mul
  [{:keys [fx/context field]}]
  (let [term (subs/mul-search-term context)]
    {:context (fx/swap-context context assoc-in [:lobby :mul] (cu/filter-units cu/mul field term str/includes?))}))

(defmethod e/event-handler ::force-selection-changed
  [{:keys [fx/context fx/event]}]
  (let [new-lobby (merge (subs/lobby context)
                         {:active-force (:unit-group/keyword event)
                          :force-zone (str (:unit-group/deployment event))
                          :force-name (:unit-group/name event)
                          :force-camo (:unit-group/camo event)})]
    {:context (fx/swap-context context assoc :lobby new-lobby)}))

(defmethod e/event-handler ::unit-selection-changed
  [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [:lobby :active-unit] (:id event))})
