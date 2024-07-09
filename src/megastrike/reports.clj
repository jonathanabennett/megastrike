(ns megastrike.reports
  (:require [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]))

(defn generate-attack-info
  [units current-force board]
  (loop [ret ""
         attackers (filter #(:target %) current-force)]
    (if (empty? attackers)
      ret
      (let [attacker (first attackers)
            target (get units (:target attacker))
            atk-data (cu/calculate-to-hit attacker target board)]
            
        (recur (str ret (:full-name attacker) " will attack " (:full-name target) ": " (cu/return-to-hit atk-data) "+ To Hit\n"
                    "Modifiers: " (cu/write-to-hit atk-data) "\n"
                    "Damage: " (cu/calculate-damage attacker (hex/hex-distance attacker target) true) "\n")
               (rest attackers))))))