(ns megastrike.reports
  (:require [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]))

(defn attack-confirmation-choices
  [attacker target board]
  (let [atk-data (cu/calculate-to-hit attacker target board)
        range (hex/hex-distance attacker target)
        regular-damage (cu/print-damage attacker range false)
        physical-damage (cu/print-damage attacker range true)]
    [{:regular (str "To Hit: " (cu/return-to-hit atk-data) "+ - " regular-damage " damage")}
     (when (= range 1)
       {:physical (str "To Hit: " (cu/return-to-hit atk-data) "+ - " physical-damage " damage")})]))

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
                    "Damage: " (cu/print-damage attacker (hex/hex-distance attacker target) (:physical attacker)) "\n")
               (rest attackers))))))