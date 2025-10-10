(ns megastrike.battle-force
  (:require
   [clojure.spec.alpha :as s]
   [megastrike.utils :as utils]
   [megastrike.combat-unit :as cu]))

(defn ->battle-force
  [force-name deployment camo team player subgroups]
  (let [zone (if deployment (keyword "direction" (utils/keyword-maker deployment)) :deployment/any)]
    (s/assert :unit-group/battleforce {:unit-group/keyword (keyword (utils/keyword-maker force-name))
                                       :unit-group/name force-name
                                       :unit-group/deployment zone
                                       :unit-group/camo camo
                                       :unit-group/parent team
                                       :unit-group/subgroups subgroups
                                       :unit-group/player player})))

(defn select-force
  [force-list force-keyword]
  (first (filter #(= (:unit-group/keyword %) force-keyword) force-list)))

(defn update-battle-force
  ([force-list force-keyword k new-value]
   (map (fn [unit-group]
          (if (= (:unit-group/keyword unit-group) force-keyword)
            (assoc unit-group k new-value)
            unit-group))
        force-list))
  ([force-list force-keyword new-force]
   (if (select-force force-list force-keyword)
     (map (fn [unit-group]
            (if (= (:unit-group/keyword unit-group) force-keyword)
              (merge unit-group new-force)
              unit-group))
          force-list)
     (conj force-list new-force))))

(defn force-units
  [battle-force units]
  ((:unit-group/keyword battle-force) (group-by :unit/battle-force units)))

(defn force-pv
  [battle-force units]
  (reduce + (map #(cu/pv %) (force-units battle-force units))))
