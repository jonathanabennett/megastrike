(ns megastrike.gui.reports
  (:require [megastrike.attacks :as attacks]))

(defn parse-attack-data 
  [{:keys [attacker target damage targeting-data to-hit result crit] :as data}]
  (let [atk-id (:id attacker)
        tgt-id (:id target)
        arm (:current-armor target (:armor target))
        penetration (- damage arm)
        target-num (attacks/calculate-to-hit targeting-data)]
    (str atk-id " attacks " tgt-id ". Needs a " target-num ".\n"
         "Rolled a " (:to-hit data) "\n"
         (if (<= target-num to-hit)
           (str "Attack hits for " damage " damage against " arm " armor.\n"
                (when (pos? penetration)
                  (str penetration " damage penetrates. " (:current-structure result) " structure remaining.\n"))
                (when (or (= to-hit 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if crit (name crit) "no critical") " on the critical hits table.\n")))
           "Attack misses.\n")
         \newline\newline\newline)))