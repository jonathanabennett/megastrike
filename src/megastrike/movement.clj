(ns megastrike.movement
  "This namespace handles all movement logic in the game. This includes
  calculating the move path, calculating the current MV for each movement-mode
  and calculating the TMM, keeping track of all of this in an internal map which is created via the `->movement` method.
  `directions` defines the different directions units can face, as well as their rear arcs when in a given facing.
  
  `get-modes` gets all the movement modes for a movement map.
  `has-mode`"
  (:require
   [clojure.math :as math]
   [clojure.string :as str]
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

(defn deployed?
  "Checks if a unit has been deployed by checking if it has a location."
  [{:keys [location]}]
  (:q location))

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
  ([{:keys [selected default]} no-default?]
   (if no-default?
     selected
     (or selected default))))

(defn set-facing
  [movement facing]
  (assoc movement :facing facing))

(defn get-facing
  [{:keys [facing]}]
  (get directions facing))

(defn get-rear [{:keys [facing]}]
  (get-in directions [facing :rear]))

(defn get-location
  [{:keys [location]}]
  location)

(defn set-hex
  [movement new-loc]
  (assoc movement
         :location (select-keys new-loc [:p :q :r])
         :path []))

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

(defn get-path
  [{:keys [path]}]
  path)

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

(defn print-tmm
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

(defn tmm-value
  [{:keys [selected] :as movement} abilities high-heat?]
  (let [jump-mod (:value (or (abilities/has? abilities :jmpw) (abilities/has? abilities :jmps) {:value 0}))]
    (condp = selected
      :immobile -4
      :stand-still 0
      :jump (+ jump-mod (print-tmm movement high-heat?) 1)
      (print-tmm movement high-heat?))))

(defn immobilize
  [movement]
  (assoc movement :options {:immobile 0} :selected :immobile :default 0 :tmm -4))

(defn take-hit
  [movement]
  (if (< (:mv-hits movement) 3)
    (assoc movement :mv-hits (inc (:mv-hits movement)))
    (immobilize movement)))

(defn cancel-movement
  [movement]
  (assoc movement :path []))

(defn move-costs
  [{:keys [selected default path location]} board]
  (let [movement-mode (or selected default)]
    (loop [sum [(board/step-cost (board/find-hex location board)
                                 (first path)
                                 movement-mode)]
           path path]
      (if (= (count path) 1)
        sum
        (recur (conj sum (board/step-cost (first path)
                                          (second path)
                                          movement-mode))
               (rest path))))))

(defn can-move?
  "Checks whether or not a unit can move from its location to a destination."
  [{:keys [path selected] :as unit} heat board]
  (cond
    (seq path)
    (let [sum (reduce + (move-costs unit board))
          move (get-mv unit heat)]
      (<= sum move))
    (contains? #{:stand-still :immobile} selected) true
    :else false))

(defn find-path
  [{:keys [selected default location]} destination board]
  (board/astar (board/find-hex location board) destination board hex/distance (or selected default)))

(defn set-path
  [movement destination board]
  (assoc movement :path (find-path movement destination board)))

(defn move-unit
  "Moves a unit if it has a destination and can move to that destination."
  [unit heat board]
  (let [unit (if (not (:selected unit))
               (assoc unit :selected (:default unit))
               unit)]
    (cond
      (or (= (:selected unit) :stand-still) (empty? (:path unit)))
      (merge unit {:selected :stand-still :path []})
      (seq (:path unit))
      (if (can-move? unit heat board)
        (set-hex unit (last (:path unit)))
        unit))))

(defn clear-mode
  [movement]
  (assoc movement :selected nil))
