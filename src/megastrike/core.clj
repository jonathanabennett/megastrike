(ns megastrike.core
  (:require
   [megastrike.combat-unit :as units]
   [megastrike.gui.core :as gui]
   [megastrike.hexagons.hex :as hex]))

(defn -main
  "I don't do a whole lot."
  []
  (gui/launch-gui))
