(ns megastrike.phases
  (:require [clojure.math :as math]
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

(defn start-initiative-phase [{:keys [turn-number forces units]}]
  (let [forces (roll-initiative forces)
        turn-order (generate-turn-order forces (vals units))]
    {:current-phase :initiative :turn-number (inc turn-number) :forces forces :turn-order turn-order :units units}))

(defn start-deployment-phase [{:keys [forces units]}]
  {:current-phase :deployment :turn-order (generate-turn-order forces (filter #(not (:q %)) (vals units))) :units units})

(defn start-movement-phase [{:keys [forces units]}]
  (prn units)
  {:current-phase :movement :turn-order (generate-turn-order forces (vals units)) :units units})

(defn start-combat-phase [{:keys [forces units]}]
  {:current-phase :combat :turn-order (generate-turn-order forces (vals units)) :units units})

(defn start-end-phase [{:keys [forces units]}]
  {:current-phase :end :turn-order (generate-turn-order forces (vals units)) :units units})

(defn next-phase [{:keys [current-phase turn-number forces units]}]
  (prn units)
  (let [new-units (into {} (for [[k unit] units] [k (assoc unit :acted nil)]))]
    (cond 
     (= current-phase :initiative) (start-deployment-phase {:forces forces :units new-units})
     (= current-phase :deployment) (start-movement-phase {:forces forces :units new-units})
     (= current-phase :movement)   (start-combat-phase {:forces forces :units new-units})
     (= current-phase :combat)     (start-end-phase {:forces forces :units new-units})
     (= current-phase :end)        (start-initiative-phase {:forces forces :turn-number turn-number :units new-units})
     :else {})))
