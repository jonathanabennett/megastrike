(ns megastrike.server
  (:require
   ;;[com.brunobonacci.mulog :as mu]
   [megastrike.core :as core]
   [megastrike.hexagons.hex :as hex]))

(defn unoccupied
  [hex units]
  (some #(hex/same-hex hex %) units))

(defn deploy-unit
  "Check if a unit can deploy in a given hex. If they can, set their location
  to that hex. If they can't, return an error explaining why."
  [{:keys [unit]} {:keys [units turn-order] :as game-state}]
  (if (and (= (:force unit) (first turn-order))
           (unoccupied unit units))
    (let [upd (assoc unit :acted true)]
      (swap! game-state assoc
             :units (assoc units (:id upd) upd)
             :turn-order (rest turn-order)
             :active-unit nil)
      game-state)
    game-state))

(defn update-game-state
  [{:keys [state]} game-state]
  (swap! game-state merge game-state state))

(defn game-event-handler
  [{:keys [event-id] :as data}]
  (condp event-id
         :deploy-unit (deploy-unit data core/*state)
         (update-game-state data core/*state)))
