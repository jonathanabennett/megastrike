(ns megastrike.gui.subs
  (:require
   [cljfx.api :as fx]))

(defn unit-counts
  [context]
  (let [units (fx/sub-val context :units)]
    (when (seq units)
      (frequencies (map :force units)))))

(defn units-ready?
  [context]
  (> (count (fx/sub-val context :units)) 1))

(defn forces-ready?
  [context]
  (> (count (fx/sub-val context :forces)) 1))
