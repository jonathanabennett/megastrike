(ns megastrike.initiative
  (:require
   [clojure.math :as math]
   [megastrike.utils :as utils]))

(defn roll-initiative [forces]
  (let [rolls (zipmap (map :name forces) (repeatedly utils/roll2d))]
    (if-not (apply distinct? (vals rolls))
      (recur forces)
      (map #(assoc % :initiative ((:name %) rolls)) forces))))

(defn generate-turn-order [forces units]
  (let [forces (sort-by :initiative forces)]
    (loop [turn-order []
           unit-counts (frequencies (map :force units))
           current-force 0]
      (if (= (reduce + unit-counts) 0)
        turn-order
        (let [f-name (:name (nth forces current-force))
              num (math/floor-div
                   (f-name unit-counts)
                   (first (sort (vals unit-counts))))]
          (recur (conj turn-order (take num (repeat current-force)))
                 (assoc unit-counts current-force (- (current-force unit-counts) num))
                 (mod (inc current-force) (count forces))))))))
