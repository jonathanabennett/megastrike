(ns megastrike.gui.reports
  (:require
   [megastrike.combat-unit :as cu]))

(defn parse-attack-data
  [{:keys [targeting-data to-hit target-damage result]}]
  (let [atk-id (get-in targeting-data [:attacker :id])
        tgt-id (get-in targeting-data [:target :id])
        target (get result tgt-id)
        crit (cu/get-new-crits target)
        arm (cu/get-current target :armor)
        penetration (- target-damage arm)
        target-num (cu/calculate-to-hit targeting-data)]
    (str atk-id " attacks " tgt-id ". Needs a " target-num ".\n"
         "Rolled a " to-hit "\n"
         (if (<= target-num to-hit)
           (str "Attack hits for " target-damage " damage against " arm " armor.\n"
                (when (pos? penetration)
                  (str penetration " damage penetrates. " (cu/get-current target :structure) " structure remaining.\n"))
                (when (or (= to-hit 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if crit (str crit) "no critical") " on the critical hits table.\n")))
           "Attack misses.\n")
         \newline \newline \newline)))
