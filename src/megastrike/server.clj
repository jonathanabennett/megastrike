(ns megastrike.server
  (require
   [com.brunobonacci.mulog :as mu]))

(def *game-state
  (atom
   {:forces {}
    :units {}
    :active-force nil
    :active-unit nil
    :map-boards []
    :round-report ""
    :game-board []
    :map-width "1"
    :map-height "1"
    :current-phase :lobby
    :turn-number 0}))

