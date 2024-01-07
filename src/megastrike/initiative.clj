(ns megastrike.initiative
  (:require
   [clojure.math :as math]
   [megastrike.utils :as utils]))

;; TODO This currently isn't applying the initiative roll to both forces equally.
;; So when I call this, I get a seq of n maps, where each map has one of the force's
;; initiatives added in.
(defn roll-initiative [forces]
  (let [rolls (zipmap (keys forces) (repeatedly utils/roll2d))]
    (if-not (apply distinct? (vals rolls))
      (recur forces)
      (->> forces
           (map (fn [[f force]]
                  [f (assoc force :initiative (f rolls))]))
           (into {})))))

(defn generate-turn-order [forces units]
  (let [forces (sort-by :initiative (vals forces))]
    (loop [turn-order []
           unit-counts (frequencies (map :force units))
           current-force 0]
      (if (= (reduce + (vals unit-counts)) 0)
        (flatten turn-order)
        (let [fname (utils/keyword-maker (:name (nth forces current-force)))
              num (if (= (first (sort > (vals unit-counts))) 0)
                    (fname unit-counts)
                    (math/floor-div
                     (fname unit-counts)
                     (first (sort > (vals unit-counts)))))]
          (recur (conj turn-order (take num (repeat fname)))
                 (assoc unit-counts fname (- (fname unit-counts) num))
                 (mod (inc current-force) (count forces))))))))
