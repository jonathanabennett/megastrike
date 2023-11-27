(ns megastrike.core
  (:require [megastrike.hexagons.hex :as hex]
            [megastrike.combat-unit :as units]
            [megastrike.gui.core :as gui]
            ))

(defn -main
  "I don't do a whole lot."
  [args]
  (gui/launch-gui))
