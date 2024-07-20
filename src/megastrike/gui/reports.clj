(ns megastrike.gui.reports
  (:require [clojure.pprint :as pprint]
            [clojure.edn :as edn]
            [megastrike.logs :as logs]
            [clojure.string :as str]
            [megastrike.attacks :as attacks]))

;; Write a function that takes a file, opens it up, and
;; dumps it into a string  for use in the reports
(defn read-logs 
  ([file]
   (let [f (slurp file)
         edns (map #(edn/read-string {:default tagged-literal} %) (str/split-lines f))
         filtered (filter #(= (:instrumentation %) :player) edns)] 
     (prn (map :player-message filtered))
     (reduce str (map :player-message filtered))))
  ([]
   (read-logs logs/log-file)))

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