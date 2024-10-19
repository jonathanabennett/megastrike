(ns megastrike.gui.reports
  (:require [megastrike.attacks :as attacks]
            [megastrike.combat-unit :as cu]))

(defn parse-attack-data
  [{:keys [attacker target target-damage targeting-data to-hit result]}]
  (let [atk-id (:id attacker)
        tgt-id (:id target)
        crit (get-in result [tgt-id :changes :crits])
        arm (cu/get-armor target)
        penetration (- target-damage arm)
        target-num (attacks/calculate-to-hit targeting-data)]
    (str atk-id " attacks " tgt-id ". Needs a " target-num ".\n"
         "Rolled a " to-hit "\n"
         (if (<= target-num to-hit)
           (str "Attack hits for " target-damage " damage against " arm " armor.\n"
                (when (pos? penetration)
                  (str penetration " damage penetrates. " (cu/get-structure target) " structure remaining.\n"))
                (when (or (= to-hit 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if crit (str crit) "no critical") " on the critical hits table.\n")))
           "Attack misses.\n")
         \newline \newline \newline)))
