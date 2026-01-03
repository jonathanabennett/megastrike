(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.battle-force :as battle-force]
   [megastrike.combat-unit :as cu]
   [megastrike.board :as board]))

(defn client
  [context]
  (fx/sub-val context :client))

(defn connection
  [context]
  (:connection (client context)))

(defn client-player
  [context]
  (:player-id (client context)))

(defn gui
  [context]
  (fx/sub-val context :gui))

(defn dialog
  [context dialog]
  (dialog (gui context)))

(defn force-name
  [context]
  (:name (dialog context :force-creation-dialog)))

(defn force-zone
  [context]
  (:zone (dialog context :force-creation-dialog)))

(defn force-camo
  [context]
  (:camo (dialog context :force-creation-dialog)))

(defn player-type
  [context]
  (:player-id (dialog context :force-creation-dialog)))

(defn active-id
  "Returns the ID of the active unit. For use in lookups."
  [context]
  (:active-unit (gui context)))

(defn round-dialog
  [context]
  (get-in (gui context) [:dialogs :round-dialog]))

(defn attack-dialog
  [context]
  (get-in (gui context) [:dialogs :attack-dialog]))

(defn round-report
  [context]
  (:round-report (gui context)))

(defn layout
  [context]
  (:layout (gui context)))

(defn lobby-view
  [context]
  (:lobby-view (gui context)))

(defn game-view
  [context]
  (:game-view (gui context)))

(defn game
  [context]
  (fx/sub-val context :game))

(defn units
  "Returns the vector containing all units"
  [context]
  (:units (game context)))

(defn active-unit
  "Returns the actual unit which corresponds to the ID returned by `active-id'."
  [context]
  (cu/select-unit (units context) (active-id context)))

(defn forces
  [context]
  (:forces (game context)))

(defn turn-order
  [context]
  (:turn-order (game context)))

(defn active-force
  [context]
  (battle-force/select-force (forces context) (first (turn-order context))))

(defn turn-number
  [context]
  (:turn-number (game context)))

(defn phase
  [context]
  (:current-phase (game context)))

(defn units-by-force
  [context]
  (group-by :unit/battle-force (units context)))

(defn board
  [context]
  (:game-board (game context)))

(defn tiles
  [context]
  (board/tiles (board context)))

(defn map-height
  [context]
  (:map-height (game context)))

(defn map-width
  [context]
  (:map-width (game context)))

(defn title-string
  [context]
  (let [bf (active-force context)
        phase (phase context)
        turn (turn-number context)]
    (if (and bf phase turn)
      (str "Megastrike | " (:unit-group/name bf) " | " (str/capitalize (name phase)) " Phase | Turn #" turn)
      "Megastrike")))

(defn lobby
  [context]
  (fx/sub-val context :lobby))

(defn active-mul
  [context]
  (:active-mul (lobby context)))

(defn p-name
  [context]
  (:pilot-name (lobby context)))

(defn p-skill
  [context]
  (:pilot-skill (lobby context)))

(defn lobby-active-force
  [context]
  (:active-force (lobby context)))

(defn mul
  [context]
  (:mul (lobby context)))

(defn mul-search-term
  [context]
  (:mul-search-term (lobby context)))

(defn map-boards
  [context]
  (:map-boards (lobby context)))

