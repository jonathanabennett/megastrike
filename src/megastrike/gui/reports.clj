(ns megastrike.gui.reports
  (:require [megastrike.attacks :as attacks]))

(defn parse-attack-data 
  [data]
  (let [atk-id (get-in data [:attacker :id])
        tgt-id (get-in data [:target :id])
        arm (get-in data [:target :current-armor] (get-in data [:target :armor]))
        penetration (- (:damage data) arm)
        target-num (attacks/calculate-to-hit (:targeting-data data))]
    (str atk-id " attacks " tgt-id ". Needs a " target-num ".\n"
         "Rolled a " (:to-hit data) "\n"
         (if (<= target-num (:to-hit data))
           (str "Attack hits for " (:damage data) " damage against " arm " armor.\n"
                (when (pos? penetration)
                  (str penetration " damage penetrates. " (get-in data [:result :current-structure]) " structure remaining.\n"))
                (when (or (= (:to-hit data) 12) (pos? penetration))
                  (str "Possible Critical: Rolled " (if (:crit data) (name (:crit data)) "no critical") " on the critical hits table.\n")))
           "Attack misses.\n")
         \newline\newline\newline)))