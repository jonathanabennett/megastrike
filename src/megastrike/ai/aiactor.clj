(ns megastrike.ai.aiactor)

(defprotocol AIActor
  (get-units [this] "Returns the map of units controlled by this AI.")
  (get-army [this])
  (get-home-edge [this] "Returns the home edge of the AI.")
  (set-home-edge [this new-edge] "Set a new home edge.")
  (get-toughness [this] "Calculates how carefully the AI is trying to protect itself. Use to multiply possible incoming damage when selecting a movement destination.")
  (get-aggression [this] "The aggression factor to  multiply potential target damage by when selecting a movement destination.")
  (set-aggression [this new-aggression] "Setes a new aggression factor.")
  (get-target [this unit] "Returns the ID of the current target for that unit, or `nil` if there is no target.")
  (set-target [this new-target] "Sets the target to `new-target`.")
  (select-target [this options]))

(defrecord Kevin [units army home-edge toughness aggression])

