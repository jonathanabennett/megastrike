(ns megastrike.game
  (:require
   [clojure.string :as str]))

(def empty-game
  {:forces {}
   :units {}
   :active-unit nil
   :active-force nil
   :game-board []
   :current-phase -1
   :turn-number 0})

(defn new-game
  []
  (reset! *game empty-game))

(defn phase-name
  [n]
  (case n
    [-1 "Game Over"]
    [0 "Initiative Phase"]
    [1 "Deployment Phase"]
    [2 "Movement Phase"]
    [3 "Combat Phase"]
    [4 "End Phase"]))

(defn make-title
  [force phase turn]
  (str "Megastrike " force " " phase " Phase | Turn " turn)
  []
  (str "Megastrike"))