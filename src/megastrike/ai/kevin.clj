(ns megastrike.ai.kevin
  "This is Kevin. He's our basic Computer Player."
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.combat-unit :as cu]
   [megastrike.utils :as utils]
   [clojure.string :as str]))

(defn target-info
  [attacker target board layout attack]
  (let [targeting (second (cu/->targeting attacker target board layout attack))
        probability (get utils/probabilities (cu/calculate-to-hit targeting) 0)
        toughness (+ (cu/get-remaining-armor target) (* (cu/get-remaining-structure target) 2))
        damage-num (if (str/ends-with? (get targeting :damage "0") "*") 0.5 (Integer/parseInt (:damage targeting)))
        expected-damage (* damage-num (/ probability 100.0))]
    [(cu/id target)
     {:firing-solution targeting
      :expected-damage expected-damage
      :toughness toughness
      :percentage (* (/ expected-damage toughness) 100.0)}]))

(defn targeting-options
  [unit units board layout]
  (map #(target-info unit % board layout :regular) units))

(defn naive-target-selection
  [options]
  (rand-nth options))

(defn select-target
  [options]
  (naive-target-selection options))
