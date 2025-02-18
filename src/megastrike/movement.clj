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
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.board :as board]
   [megastrike.hexagons.hex :as hex]
   [megastrike.schemas :as schemas]
   [megastrike.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas and variable definitions

(def MovementSchema
  [:map
   [:options :map]
   [:tmm pos?]
   [:mv-hits number?]
   [:default (or nil? keyword?)]
   [:selected (or nil? keyword?)]
   [:location (or nil? schemas/Hexagon)]
   [:path (or nil? vector?)]
   [:facing (or nil? keyword?)]])

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
;; Movement Map Constructor and helper methods

(defn- move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :walk
      (= mv-key (utils/keyword-maker "j")) :jump
      :else (utils/keyword-maker mv-type))))

(defn- parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :jump))
      (merge mv-map {:walk (val (first mv-map))})
      mv-map)))

(defn ->movement
  [{:keys [tmm mv-hits movement selected location path facing] :or {mv-hits 0 selected nil location nil path [] facing nil}}]
  (let [options (parse-movement movement)
        default (if (contains? options :walk)
                  :walk
                  (first (keys options)))
        movement {:options options
                  :tmm (Integer/parseInt tmm)
                  :mv-hits mv-hits
                  :default default
                  :selected selected
                  :location location
                  :path path
                  :facing facing}]
    movement))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Location-based methods

(defn deployed?
  "Checks if a unit has been deployed by checking if it has a location."
  [{:keys [location]}]
  (:q location))

(defn get-location
  [{:keys [location]}]
  location)

(defn set-hex
  [movement new-loc]
  (assoc movement
         :location (select-keys new-loc [:p :q :r])
         :path []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modes (default and selected) methods

(defn get-modes
  "Gets all movement modes for a unit."
  [movement]
  (:options movement))

(defn has-mode?
  [movement mode]
  (contains? (get-modes movement) mode))

(defn set-mode
  "Sets the movement mode for a unit to mode. Does nothing if mode is not
  valid for this unit."
  [movement mode]
  (if (has-mode? movement mode)
    (assoc movement :selected mode)
    movement))

(defn selected
  ([{:keys [selected default]} default?]
   (if default?
     (or selected default)
     selected)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Facing methods

(defn set-facing
  [movement facing]
  (assoc movement :facing facing))

(defn get-facing
  [{:keys [facing]}]
  (get directions facing))

(defn get-rear
  [{:keys [facing]}]
  (get-in directions [facing :rear]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MV and TMM methods

(defn get-mv
  ([{:keys [options mv-hits]} heat move-type]
   (let [base-move (move-type options)]
     (loop [mv base-move
            n 0]
       (if (= n mv-hits)
         (max (- mv heat) 0)
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([movement heat]
   (get-mv movement heat (or (:selected movement) (:default movement)))))

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

(defn tmm-value
  "When a TMM value needs to be calculated, apply mv-hits and high heat."
  [{:keys [tmm mv-hits]} high-heat?]
  (if (pos? mv-hits)
    (loop [tmm tmm
           n 0]
      (if (= n mv-hits)
        (if high-heat?
          (dec tmm)
          tmm)
        (recur (let [new-tmm (math/round (/ tmm 2.0))]
                 (if (>= (- tmm new-tmm) 1) new-tmm 0))
               (inc n))))
    (if high-heat?
      (dec tmm)
      tmm)))

(defn tmm
  "Returns the TMM for the unit based on its selected movement."
  [{:keys [selected default] :as movement} abilities high-heat?]
  (let [jump-mod (:value (or (abilities/has? abilities :jmpw) (abilities/has? abilities :jmps) {:value 0}))
        s (or selected default)]
    (condp = s
      :immobile -4
      :stand-still 0
      :jump (+ jump-mod (tmm-value movement high-heat?) 1)
      (tmm-value movement high-heat?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MV damage methods

(defn immobilize
  [movement]
  (assoc movement :options {:immobile 0} :selected :immobile :default 0 :tmm -4))

(defn take-hit
  [movement]
  (if (< (:mv-hits movement) 3)
    (assoc movement :mv-hits (inc (:mv-hits movement)))
    (immobilize movement)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Path and movement methods

(defn- calc-approx-dist
  [h dist]
  (for [[node d] dist]
    [node (+ d (h node))]))

(defn astar [origin destination board heuristic mv-type unit-force]
  (let [guess-goal-dist #(heuristic % destination)
        tiles (board/tiles board)
        neighbors #(board/neighbors board %)
        weight #(board/step-cost %1 %2 mv-type unit-force)]
    (loop [known-dist (merge (into {} (for [x tiles] [x ##Inf]))
                             {origin 0})
           guess-unseen-dist (into (priority-map/priority-map)
                                   (calc-approx-dist guess-goal-dist known-dist))
           visited? #{}
           path {origin []}]
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
            ; (mu/log ::neighbors
            ;         :best-unseen best-unseen
            ;         :neighbors (neighbors best-unseen)
            ;         :closer-nbrs closer-nbrs
            ;         :closer-paths closer-paths)
            (recur (into known-dist closer-nbrs)
                   (into (pop guess-unseen-dist)
                         (calc-approx-dist guess-goal-dist closer-nbrs))
                   (conj visited? best-unseen)
                   (into path closer-paths))))))))

(defn get-path
  [{:keys [path]}]
  path)

(defn cancel-movement
  [movement]
  (assoc movement :path []))

(defn move-cost
  [{:keys [selected default path]} unit-force]
  (let [movement-mode (or selected default)]
    (board/path-cost path movement-mode unit-force)))

(defn unblocked-path?
  [path unit-force]
  (some #(= ((get % :stacking false)) unit-force) path))

(defn can-move?
  "Checks whether or not a unit can take a path."
  [{:keys [selected location] :as unit} path heat unit-force]
  (cond
     ;; This path doesn't start at the unit's location
    (not (hex/same-hex (first path) location)) false
    ;; The path ends at an occupied hex
    (get (last path) :stacking false) false
    ;; The path crosses a hex occupied by an enemy unit
    (unblocked-path? path unit-force) false
    ;; Units standing still or immobile should have no path
    (and (contains? #{:stand-still :immobile} selected) (empty? path)) true
    (pos? (count path)) (<= (board/path-cost path selected unit-force) (get-mv unit heat))
    :else false))

(defn find-path
  "Finds a path to a given destination, regardless of whether or not the unit has the MV to get there.
  IF the hex is blocked (i.e. the final cost is ##Inf), it will progressively try (butlast path) to try to get as close as possible.
  NOTE: THIS METHOD ASSUMES THE LOCATIONS OF UNITS HAVE ALREADY BEEN MARKED."
  [{:keys [selected default location] :as unit} unit-force heat destination board]
  (loop [path (astar location destination board hex/distance (or selected default) unit-force)]
    (if (or (empty? path) (can-move? unit path heat unit-force))
      path
      (recur (butlast path)))))

(defn set-path
  [movement destination unit-force heat board]
  (assoc movement :path (find-path movement unit-force heat destination board)))

(defn move-unit
  "Moves a unit if it has a destination and can move to that destination."
  [unit heat unit-force]
  (let [u (if (not (:selected unit))
            (assoc unit :selected (:default unit))
            unit)]
    (if (can-move? u (:path unit) heat unit-force)
      (set-hex u (last (:path unit)))
      unit)))

(defn clear-mode
  [movement]
  (assoc movement :selected nil))
