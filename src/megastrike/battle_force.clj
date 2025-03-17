(ns megastrike.battle-force
  (:require
   [megastrike.utils :as utils]))

(defprotocol UnitGroup
  (to-str [this] "Returns a representation of this force for printing.")
  (id [this] "Returns the name of the force as a string.")
  (set-name [this new-name] "Sets the name.")
  (get-home-edge [this] "Set the home edge")
  (set-home-edge [this new-home-edge] "Set the home edge.")
  (get-deployment [this] "Get the deployment.")
  (set-deployment [this new-deployment] "Set the deployment.")
  (get-camo [this] "Get the camo image.")
  (set-camo [this new-camo] "Set the camo image.")
  (get-team [this] "Get the team.")
  (set-team [this new-team] "Set the team.")
  (get-player [this] "Get the player (either a :player keyword or an AI object.")
  (set-player [this new-player] "Set the player to either :player or an AI object.")
  (same-team? [this other] "Returns whether or not this Force and another Force are on the same team."))

(defrecord BattleForce [force-keyword force-name deployment camo team player]
  UnitGroup
  (to-str [this] (str (:force-name this)))
  (id [this] (:force-keyword this))
  (set-name [this new-name] (assoc this :force-name new-name))
  (get-deployment [this] (:deployment this))
  (set-deployment [this new-deployment] (assoc this :deployment new-deployment))
  (get-camo [this] (:camo this))
  (set-camo [this new-camo] (assoc this :camo new-camo))
  (get-team [this] (:team this))
  (set-team [this new-team] (assoc this :team new-team))
  (get-player [this] (:player this))
  (set-player [this new-player] (assoc this :player new-player))
  (same-team? [this other] (= (get-team this) (get-team other))))

(defn create-force
  [force-name deployment camo team player]
  (->BattleForce (utils/keyword-maker force-name) force-name  deployment camo team player))
