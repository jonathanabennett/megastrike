(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.battle-force :as battle-force]
   [megastrike.combat-unit :as cu]
   [megastrike.board :as board]))

(defn game
  [context]
  (fx/sub-val context :game))

(defn gui
  [context]
  (fx/sub-val context :gui))

(defn units
  "Returns the vector containing all units"
  [context]
  (fx/sub-val context :game :units))

(defn active-id
  "Returns the ID of the active unit. For use in lookups."
  [context]
  (fx/sub-val context :game :active-unit))

(defn active-unit
  "Returns the actual unit which corresponds to the ID returned by `active-id'."
  [context]
  (cu/select-unit (units context) (active-id context)))

(defn forces
  [context]
  (fx/sub-val context :game :forces))

(defn active-force
  [context]
  (battle-force/select-force (forces context) (first (fx/sub-val context :turn-order))))

(defn round-report
  [context]
  (fx/sub-val context :gui :round-report))

(defn turn-number
  [context]
  (fx/sub-val context :game :turn-number))

(defn phase
  [context]
  (fx/sub-val context :game :current-phase))

(defn turn-order
  [context]
  (fx/sub-val context :game :turn-order))

(defn title-string
  [context]
  (let [bf (active-force context)
        phase (phase context)
        turn (turn-order context)]
    (if (and bf phase turn)
      (str "Megastrike | " (:unit-group/name bf) " | " (str/capitalize (name phase)) " Phase | Turn #" turn)
      "Megastrike")))

(defn units-by-force
  [context]
  (group-by :unit/battle-force (units context)))

(defn layout
  [context]
  (fx/sub-val context :gui :layout))

(defn board
  [context]
  (fx/sub-val context :game :game-board))

(defn tiles
  [context]
  (board/tiles (board context)))
