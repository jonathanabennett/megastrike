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
   [clojure.string :as str]
   [clojure.string :as string]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.board :as board]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :as utils]))

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

(defprotocol Moveable
  (get-modes [this] "Returns a vector of this unit's movement modes.")
  (has-mode? [this mode] "Returns whether or not this unit has a given movement mode.")
  (get-default [this] "Gets the default movement value of a unit.")
  (get-selected [this] [this accept-default?] "Gets the currently selected movement mode. If called with accept-default? It will return either selected movement mode or the default if none is selected.")
  (set-selected [this new-selected] "Sets the units selected movement mode to new-selected.")
  (select-default [this] "Sets the units selected movement mode to its default IF something hasn't already been selected.")
  (clear-selected [this] "Clears the selected movement mode, leaving it empty.")

  (get-location [this] "Returns the hex address of the unit.")
  (set-location [this new-location] "Updates the unit's location on the map to a new hex.")
  (deployed? [this] "Returns true if the unit is deployed.")
  (get-facing [this] "Returns the facing map (selected from `directions`) that the unit is facing.")
  (get-rear [this] "Returns the unit's rear based on its facing map.")
  (set-facing [this new-facing] "Changes the units facing to new-facing.")

  (get-hits [this] "Returns the number of mv hits a unit has taken.")
  (immobilize [this])
  (take-hit [this] "Take a movement hit.")

  (get-mv [this heat] [this heat mv-type] "Gets the move value for a unit. When no mv-type is provided, should return either the selected or default.")
  (get-tmm [this high-heat?] "Returns the TMM of a unit, adjusted for high heat.")
  (get-tmm-data [this abilities high-heat?] "Returns the tmm-value object used for attacks.")

  (cancel-movement [this] "Cancels the unit's planned movement.")
  (can-move? [this heat unit-force] [this path heat unit-force] "Returns true if a unit can actually move along the path it has selected.")

  (get-path [this] "Returns the path the unit is planning to follow.")
  (find-path [this heat unit-force destination board] "Returns the path that gets the unit as close as possible to its destination when accounting for stacking and its move value.
             NOTE: this assumes that board has had stacking already marked on it.")
  (set-path [this heat unit-force destination board] [this path] "Set the unit's path to the path returned by calling find-path with destination.")

  (move-unit [unit heat unit-force] "Changes the unit's location based on the path and sets the path to `[]`"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement Map Constructor and helper methods

(defn- move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :move/walk
      (= mv-key (utils/keyword-maker "j")) :move/jump
      :else (keyword "move" (-> mv-type
                                (string/trim)
                                (string/lower-case)
                                (utils/remove-parens)
                                (utils/correct-range-brackets)
                                (utils/replace-spaces))))))

(defn- parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :jump))
      (merge mv-map {:walk (val (first mv-map))})
      mv-map)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MV and TMM methods

(defn print-movement-helper
  "Consumes a vector containing a move type as a keyword and a distance and prints it for human consumption."
  [unit mv-vec heat]
  (cond
    (= (first mv-vec) :walk) (get-mv unit heat (first mv-vec))
    (= (first mv-vec) :jump) (str (get-mv unit heat (first mv-vec)) "j")
    :else (str (get-mv unit heat (first mv-vec)) (name (first mv-vec)))))

(defn print-movement
  "Loops over all movements a unit has a pretty prints them."
  [unit heat]
  (str/join "/" (map #(print-movement-helper unit % heat) (get-modes unit))))

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
  [{:keys [selected default path]} unit-force]
  (let [movement-mode (or selected default)]
    (board/path-cost path movement-mode unit-force)))

(defn unblocked-path?
  [path unit-force]
  (some #(not= (get % :stacking false) unit-force) path))

(defn selected-or-default [unit]
  (or (:move/selected unit) (:move/default unit)))

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

(defn available-mv
  ([u mv-type]
   (let [base-move (mv-type (:unit/move-modes u))
         mv-hits (count (filter #(= :crits/mv (:crits/taken (:unit/criticals %))) u))]
     (loop [mv base-move
            n 0]
       (if (= n mv-hits)
         (max (- mv (:unit/current-heat u)) 0)
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([u]
   (available-mv u (selected-or-default u))))

(defn base-tmm
  [u]
  (let  [ret (loop [value (:unit/tmm u)
                    n 0]
               (if (= n (count (filter #(= :crits/mv (:crits/taken (:unit/criticals %))) u)))
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

(defrecord MechMovement [modes tmm mv-hits selected default location path facing]
  Moveable
  (get-modes [this] (:modes this))
  (has-mode? [this mode] (contains? (get-modes this) mode))
  (get-default [this] (:default this))
  (get-selected [this] (:selected this))
  (get-selected [this accept-default?] (if (and accept-default? (not (:selected this)))
                                         (get-default this)
                                         (get-selected this)))
  (set-selected [this new-mode] (assoc this :selected new-mode))
  (select-default [this]
    (if (get-selected this)
      this
      (set-selected this (get-default this))))
  (clear-selected [this] (assoc this :selected nil))

  (get-location [this] (:location this))
  (set-location [this new-location] (assoc this :location (select-keys new-location [:p :q :r])))
  (deployed? [this] (get (get-location this) :q false))
  (get-facing [this] (get directions (:facing this)))
  (get-rear [this] (:rear (get-facing this)))
  (set-facing [this new-facing] (assoc this :facing new-facing))

  (get-hits [this] (:mv-hits this))
  (immobilize [this] (assoc this :modes {:immobilized 0} :default :immobilized))
  (take-hit [this] (if (< (get-hits this) 3)
                     (assoc this :mv-hits (inc (get-hits this)))
                     (immobilize this)))

  (get-mv [this heat move-type] (let [base-move (move-type (get-modes this))]
                                  (loop [mv base-move
                                         n 0]
                                    (if (= n mv-hits)
                                      (max (- mv heat) 0)
                                      (recur (let [new-mv (math/round (/ mv 2.0))]
                                               (if (>= (- mv new-mv) 1) new-mv 0))
                                             (inc n))))))
  (get-mv [this heat] (get-mv this heat (get-selected this)))
  (get-tmm [this high-heat?] (let [tmm (loop [value (:tmm this)
                                              n 0]
                                         (if (= n mv-hits)
                                           value
                                           (recur (let [new-tmm (math/round (/ tmm 2.0))]
                                                    (if (>= (- value new-tmm) 1) new-tmm 0))
                                                  (inc n))))]
                               (if high-heat?
                                 (dec tmm)
                                 tmm)))
  (get-tmm-data [this abilities high-heat?]
    (let [jump-mod (:value (or (abilities/has? abilities :jmpw) (abilities/has? abilities :jmps) {:value 0}))
          s (get-selected this true)]
      (condp = s
        :immobile -4
        :stand-still 0
        :jump (+ jump-mod (get-tmm this high-heat?) 1)
        (get-tmm this high-heat?))))

  (cancel-movement [this] (assoc this :path [] :selected false))
  (can-move? [this heat unit-force]
    (can-move? this (get-path this) heat unit-force))
  (can-move? [this path heat unit-force]
    (cond
     ;; This path doesn't start at the unit's location
      (not (hex/same-hex (first path) (get-location this))) (do (mu/log ::move-failed
                                                                        :reason "path doesn't start at the unit's location.") false)
    ;; The path ends at an occupied hex
      (get (last path) :stacking false) (do (mu/log ::move-failed
                                                    :reason "Path ends in an occupied hex.") false)
    ;; The path crosses a hex occupied by an enemy unit

      ; (unblocked-path? path unit-force) (do (mu/log ::move-failed
      ;                                               :reason "Path crosses a hex occupied by an enemy unit.") false)

    ;; Units standing still or immobile should have no path
      (and (contains? #{:stand-still :immobile} (get-selected this false)) (empty? path)) true
      (pos? (count path)) (<= (reduce + (board/path-cost path (get-selected this false) unit-force)) (get-mv this heat))
      :else (do (mu/log ::move-failed
                        :reason "unknown"
                        :unit this
                        :path path
                        :heat heat
                        :unit-force unit-force)
                false)))

  (get-path [this] (:path this))
  (find-path [this heat unit-force destination board]
    (loop [path (astar (get-location this) destination board hex/distance (get-selected this true) unit-force)]
      (if (or (empty? path) (can-move? this path heat unit-force))
        path
        (recur (butlast path)))))

  (set-path [this heat unit-force destination board]
    (select-default (assoc this :path (find-path (select-default this) heat unit-force destination board))))
  (set-path [this path]
    (-> this
        (select-default)
        (assoc :path path)))

  (move-unit [this heat unit-force]
    (let [u (if (not (get-selected this false))
              (set-selected this (get-default this))
              this)]
      (if (can-move? u (get-path this) heat unit-force)
        (-> u
            (set-location (last (get-path this)))
            (assoc :path []))
        this))))

(defn create-movement
  [{:keys [tmm mv-hits movement selected location path facing] :or {mv-hits 0 selected nil location nil path [] facing nil}}]
  (let [options (parse-movement movement)
        default (if (contains? options :walk)
                  :walk
                  (first (keys options)))]
    (->MechMovement options (Integer/parseInt tmm) mv-hits selected default location path facing)))
