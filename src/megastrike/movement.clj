(ns megastrike.movement
  "This namespace handles all movement logic in the game. This includes
  calculating the move path, calculating the current MV for each movement-mode
  and calculating the TMM, keeping track of all of this in an internal map which is created via the `->movement` method.
  `directions` defines the different directions units can face, as well as their rear arcs when in a given facing.
  
  `get-modes` gets all the movement modes for a movement map.
  `has-mode`"
  (:require
   [clojure.data.priority-map :as priority-map]
   [clojure.math :as math]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.board :as board]
   [megastrike.damage :as damage]
   [megastrike.hexagons.hex :as hex]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas and variable definitions

(def directions
  {:n  {:angle 0
        :ordinal 2
        :points [8 9 10 11]
        :rear :s}
   :ne {:angle 60
        :ordinal 1
        :points [10 11 0 1]
        :rear :sw}
   :se {:angle 120
        :ordinal 0
        :points [0 1 2 3]
        :rear :nw}
   :s  {:angle 180
        :ordinal 5
        :points [2 3 4 5]
        :rear :n}
   :sw {:angle 240
        :ordinal 4
        :points [4 5 6 7]
        :rear :ne}
   :nw {:angle 300
        :ordinal 3
        :points [6 7 8 9]
        :rear :se}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MV and TMM methods

(defn has-mode?
  [unit mode]
  (contains? (:unit/move-modes unit) mode))

(defn selected-or-default [unit]
  (or (:move/selected unit) (:move/default unit)))

(defn available-mv
  ([u mv-type]
   (let [base-move (mv-type (:unit/move-modes u))
         mv-hits (damage/crit-count u :crit/mv)]
     (loop [mv base-move
            n 0]
       (if (= n mv-hits)
         (max (- mv (:unit/current-heat u)) 0)
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([u]
   (available-mv u (selected-or-default u))))

(defn print-movement-helper
  "Consumes a vector containing a move type as a keyword and a distance and prints it for human consumption."
  [unit mv-vec]
  (cond
    (= (first mv-vec) :walk) (available-mv (first mv-vec))
    (= (first mv-vec) :jump) (str (available-mv (first mv-vec)) "j")
    :else (str (available-mv unit (first mv-vec)) (name (first mv-vec)))))

(defn print-movement
  "Loops over all movements a unit has a pretty prints them."
  [unit]
  (string/join "/" (map #(print-movement-helper unit %) (:unit/move-modes unit))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Path and movement methods

(defn- calc-approx-dist
  [h dist]
  (for [[node d] dist]
    [node (+ d (h node))]))

(defn astar
  [origin destination board heuristic mv-type unit-force]
  (let [guess-goal-dist #(heuristic % destination)
        tiles (board/tiles board)
        neighbors #(board/neighbors board %)
        weight #(board/step-cost %1 %2 mv-type unit-force)]
    (loop [known-dist (merge (into {} (for [x tiles] [x ##Inf]))
                             {origin 0})
           guess-unseen-dist (into (priority-map/priority-map)
                                   (calc-approx-dist guess-goal-dist known-dist))
           visited? #{}
           path {origin [origin]}]
      (let [[best-unseen _] (peek guess-unseen-dist)]
        (if (or (= (known-dist best-unseen) ##Inf)
                (visited? destination)
                (empty? guess-unseen-dist))
          (if destination (path destination) path)
          (let [closer-nbrs (for [nbr (neighbors best-unseen)
                                  :let [new-known-dist (+ (known-dist best-unseen)
                                                          (weight best-unseen nbr))]
                                  :when (< new-known-dist (known-dist nbr))]
                              [nbr new-known-dist])
                closer-paths (for [[nbr _] closer-nbrs]
                               [nbr (conj (path best-unseen) nbr)])]
            (recur (into known-dist closer-nbrs)
                   (into (pop guess-unseen-dist)
                         (calc-approx-dist guess-goal-dist closer-nbrs))
                   (conj visited? best-unseen)
                   (into path closer-paths))))))))

(defn move-cost
  [{:keys [unit/selected unit/default unit/path unit/battle-force]}]
  (let [movement-mode (or selected default)]
    (board/path-cost path movement-mode battle-force)))

(defn unblocked-path?
  [path unit-force]
  (some #(not= (get % :stacking false) unit-force) path))

(defn set-location [u hex]
  (assoc u :unit/location (select-keys hex [:p :q :r])))

(defn deployed? [u]
  (get-in u [u :unit/location :q] false))

(defn facing [u]
  (get directions (:unit/facing u)))

(defn rear [u]
  (:rear (facing u)))

(defn change-facing [u new-facing]
  (when (s/assert :unit/facing new-facing)
    (assoc u :facing new-facing)))

(defn immobilize [u]
  (assoc u :unit/move-modes {:move/immobilized 0} :default :immobilized))

(defn base-tmm
  [u]
  (let  [ret (loop [value (:unit/tmm u)
                    n 0]
               (if (= n (damage/crit-count u :crit/mv))
                 value
                 (recur (let [new-tmm (math/round (/ value 2.0))]
                          (if (>= (- value new-tmm) 1) new-tmm 0))
                        (inc n))))]
    (if (<= 2 (:unit/current-heat u))
      (dec ret)
      ret)))

(defn modified-tmm
  [u]
  (let [jump-mod (:value (or (abilities/has? (:unit/abilities u) :jmpw) (abilities/has? (:unit/abilities u) :jmps)) {:value 0})
        move-mode (selected-or-default u)]
    (condp = move-mode
      :move/immobilized -4
      :move/stand-still 0
      :move/jump (+ jump-mod (base-tmm u))
      (base-tmm u))))

(defn cancel-move
  [u]
  (assoc u :unit/path [] :unit/selected false))

(defn can-move?
  ([u path]
   (cond
     (not (hex/same-hex (first path) (:location u)))
     (do (mu/log ::move-failed
                 :reason "Path doesn't start at unit's location.")
         false)
     (get (last path) :stacking false)
     (do (mu/log ::move-failed
                 :reason "Path ends in an occupied hex.")
         false)

     (and (contains? {:move/stand-still :move/immobilized} (selected-or-default u)) (empty? path)) true
     (pos? (count path)) (<= (reduce + (board/path-cost path (selected-or-default u) (:unit/battle-force u)))
                             (available-mv u))
     :else (do (mu/log ::move-failed
                       :reason "Unknown"
                       :unit u
                       :path path)
               false)))
  ([u]
   (can-move? u (:path u))))

(defn find-path
  [u destination board]
  (loop [path (astar (:unit/location u) destination board hex/distance (selected-or-default u) (:unit/battle-force u))]
    (if (or (empty? path) (can-move? u path))
      path
      (recur (butlast path)))))

(defn set-path
  [u destination board]
  (let [moving-unit (if (:unit/selected u) u (assoc u :unit/selected (:unit/default u)))
        path (find-path u destination board)]
    (if (seq? path)
      (assoc moving-unit :unit/path path)
      u)))

(defn move-unit
  [u]
  (let [moving-unit (if (:unit/selected u) u (assoc u :unit/selected (:unit/default u)))]
    (if (can-move? moving-unit)
      (assoc moving-unit :unit/location (last (:unit/path moving-unit)) :unit/path [] :unit/acted? true)
      u)))

