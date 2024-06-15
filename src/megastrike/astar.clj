(ns megastrike.astar
  (:require [clojure.math :as math]
            [clojure.set :as set]
            [megastrike.hexagons.hex :as hexagon]
            [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]))

(defn reconstruct-path
  [came-from current]
  (loop [current current
         path (list current)]
    (if (contains? came-from current)
      (let [next-node (get came-from current)]
        (recur next-node (conj path next-node)))
      path)))

(defn score
  [m node]
  (get m node Double/POSITIVE_INFINITY))

(defn astar 
  [world start end mv-type]
  (loop [closed #{}
         open #{start}
         came-from {}
         g-score {start 0}
         f-score {start (hexagon/hex-distance start end)}]
    (if (empty? open)
      :failure
      (let [current (first (sort-by #(score f-score %) open))]
        (if (hexagon/same-hex current end)
          (reconstruct-path came-from current)
          (let [open (set (remove #{current} open))
                closed (conj closed current)
                neighbors (set (remove closed (hexagon/hex-neighbors current)))
                {:keys [came-from g-score f-score]} (reduce 
                                                     (fn [{:keys [came-from g-score f-score] :as acc} neighbor]
                                                       (let [maybe-g-score (+ (score g-score current) (hexagon/step-cost current neighbor mv-type))]
                                                         (if (>= maybe-g-score (score g-score neighbor))
                                                           acc
                                                           {:came-from (assoc came-from neighbor current)
                                                            :g-score (assoc g-score neighbor maybe-g-score)
                                                            :f-score (assoc f-score neighbor (+ (score g-score neighbor) (hexagon/hex-distance neighbor end)))})))
                                                     {:came-from came-from
                                                      :g-score g-score 
                                                      :f-score f-score}
                                                     neighbors)]
            (recur closed
                   (set/union open neighbors)
                   came-from
                   g-score 
                   f-score)))))))