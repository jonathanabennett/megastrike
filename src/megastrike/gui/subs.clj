(ns megastrike.gui.subs
  (:require [cljfx.api :as fx]
            [clojure.string :as str]))

(defn title-string
  [context]
  (let [force (first (fx/sub-val context :turn-order))
        phase (fx/sub-val context :current-phase)
        turn (fx/sub-val context :turn-number)]
    (if (and force phase turn) 
      (str "Megastrike | " (str/capitalize (name force)) " | " (str/capitalize (name phase)) " Phase | Turn #" turn)
      (str "Megastrike"))))

(defn units
  [context]
  (fx/sub-val context :units))

(defn undeployed-units
  [context]
  (filter #(not (:q %)) (vals (units context))))

(defn deployed-units 
  [context]
  (filter #(:q %) (vals (units context))))

(defn unit-ghosts 
  [context]
  (fx/sub-val context :ghosts))

(defn forces 
  [context]
  (fx/sub-val context :forces))

(defn turn-number
  [context]
  (fx/sub-val context :turn-number))

(defn phase 
  [context]
  (fx/sub-val context :current-phase))

(defn turn-order
  [context]
  (fx/sub-val context :turn-order))

(defn units-by-force
  [context]
  (group-by :force (vals (units context))))
