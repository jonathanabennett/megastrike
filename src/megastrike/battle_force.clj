(ns megastrike.battle-force
  (:require
   [clojure.spec.alpha :as s]
   [megastrike.utils :as utils]
   [megastrike.combat-unit :as cu]))

(defn ->battle-force
  [force-name deployment camo team player]
  (let [zone (if deployment (keyword "direction" (utils/keyword-maker deployment)) :deployment/any)]
    (s/assert :unit-group/battleforce {:unit-group/keyword (keyword (utils/keyword-maker force-name))
                                       :unit-group/name force-name
                                       :unit-group/deployment zone
                                       :unit-group/camo camo
                                       :unit-group/parent team
                                       :unit-group/player player})))

(defn force-units
  [battle-force units]
  ((:unit-group/keyword battle-force) (group-by :unit/battle-force (vals units))))

(defn force-pv
  [battle-force units]
  (reduce + (map #(cu/pv %) (force-units battle-force units))))
