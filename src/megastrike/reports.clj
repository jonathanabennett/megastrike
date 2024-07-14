(ns megastrike.reports
  (:require [megastrike.combat-unit :as cu]
            [megastrike.attacks :as attacks]
            [megastrike.hexagons.hex :as hex]))

(defn attack-confirmation-choices
  [attacker target board]
  (let [atk-data (attacks/produce-attack-roll attacker target board)
        range (hex/hex-distance attacker target)
        regular-damage (cu/print-damage attacker range false)
        physical-damage (cu/print-damage attacker range true)]
    [{:regular (str (attacks/print-attack-roll atk-data false) ": " regular-damage " damage")}
     (when (= range 1)
       {:physical (str (attacks/print-attack-roll atk-data false) ": " physical-damage " damage")})]))

(defn generate-attack-info
  [units current-force board]
  (loop [ret ""
         attackers (filter #(:target %) current-force)]
    (if (empty? attackers)
      ret
      (let [attacker (first attackers)
            target (get units (:target attacker))
            atk-data (attacks/produce-attack-roll attacker target board)]
        (recur (str ret (:full-name attacker) " will attack " (:full-name target) ": " (attacks/calculate-to-hit atk-data) "\n"
                    "Modifiers: " (attacks/print-attack-roll atk-data) "\n"
                    "Damage: " (cu/print-damage attacker (hex/hex-distance attacker target) (:physical attacker)) "\n")
               (rest attackers))))))