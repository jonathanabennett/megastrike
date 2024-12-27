(ns megastrike.heat)

(defn ->heat [current-heat overheat]
  {:current current-heat
   :overheat overheat})

(defn add [{:keys [current] :as heat}]
  (if (<= current 3)
    (assoc heat :current (inc current))
    heat))

(defn current [heat]
  (get heat :current 0))
