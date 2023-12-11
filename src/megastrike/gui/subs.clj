(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]
   [megastrike.combat-unit :as cu]))

(defn units
  [context]
  (fx/sub-val context :units))

(defn units-ready?
  [context]
  (> (count (fx/sub-val context :units)) 1))

(defn forces-ready?
  [context]
  (> (count (fx/sub-val context :forces)) 1))

(defn units-by-force
  [context]
  (group-by :force (fx/sub-val context :units)))
