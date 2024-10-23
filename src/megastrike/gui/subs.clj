(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]))

(defn title-string
  [context]
  (let [force (first (fx/sub-val context :turn-order))
        phase (fx/sub-val context :current-phase)
        turn (fx/sub-val context :turn-number)]
    (if (and force phase turn)
      (str "Megastrike | " (str/capitalize (name force)) " | " (str/capitalize (name phase)) " Phase | Turn #" turn)
      (str "Megastrike"))))

(defn units
  "Returns the map containing all units from the context. The map key is generated by running `(:id u)` where u is the unit map."
  [context]
  (fx/sub-val context :units))

(defn undeployed-units
  [context]
  (filter #(not (:q %)) (vals (units context))))

(defn active-id
  "Returns the ID of the active unit. For use in lookups."
  [context]
  (fx/sub-val context :active-unit))

(defn active-unit
  "Returns the actual unit which corresponds to the ID returned by `active-id'."
  [context]
  (get (units context) (active-id context)))

(defn deployed-units
  [context]
  (filter #(:q %) (vals (units context))))

(defn forces
  [context]
  (fx/sub-val context :forces))

(defn turn-number
  [context]
  (fx/sub-val context :turn-number))

(defn phase
  [context]
  (fx/sub-val context :current-phase))

(defn turn-order
  [context]
  (fx/sub-val context :turn-order))

(defn current-forces
  [context]
  (filter #(= (:force %) (first (turn-order context))) (vals (units context))))

(defn units-by-force
  [context]
  (group-by :force (vals (units context))))

(defn get-view
  [context]
  (fx/sub-val context :display))

(defn layout
  [context]
  (fx/sub-val context :layout))

(defn board
  [context]
  (fx/sub-val context :game-board))
