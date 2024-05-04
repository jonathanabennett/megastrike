(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.combat-unit :as cu]))

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
  (vals (fx/sub-val context :units)))

(defn units-ready?
  [context]
  (> (count (fx/sub-val context :units)) 1))

(defn forces-ready?
  [context]
  (> (count (fx/sub-val context :forces)) 1))

(defn units-by-force
  [context]
  (group-by :force (vals (fx/sub-val context :units))))
