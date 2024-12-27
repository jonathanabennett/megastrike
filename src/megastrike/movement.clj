(ns megastrike.movement
  "This namespace handles all movement logic in the game. This includes
  calculating the move path, calculating the current MV for each movement-mode
  and calculating the TMM, keeping track of all of this in an internal map which is created via the `->movement` method."
  (:require
   [clojure.math :as math]
   [megastrike.abilities :as abilities]
   [megastrike.board :as board]
   [megastrike.heat :as heat]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :as utils]))

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

(defn get-modes
  [movement]
  (:options movement))

(defn has-method?
  [movement method]
  (contains? (get-modes movement) method))

(defn ->movement
  [{:keys [tmm mv-hits movement selected location path facing] :or {mv-hits 0 selected nil location nil path [] facing nil}}]
  (let [options (parse-movement movement)
        default (if (contains? options :walk)
                  :walk
                  (first (keys options)))]
    (assert (not (empty? options)))
    {:tmm (Integer/parseInt tmm)
     :mv-hits mv-hits
     :options options
     :default default
     :selected selected
     :location location
     :path path
     :facing facing}))

(defn set-facing [movement facing]
  (assoc movement :facing facing))

(defn get-rear [{:keys [facing]}]
  (get-in directions [facing :rear]))

(defn get-mv
  ([{:keys [options mv-hits]} heat move-type]
   (let [base-move (move-type options)]
     (loop [mv base-move
            n 0]
       (if (= n mv-hits)
         (max (- mv (heat/current heat)) 0)
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([movement heat]
   (get-mv movement heat (:selected movement))))

(defn print-movement-helper
  "Consumes a vector containing a move type as a keyword and a distance and prints it for human consumption."
  [mv-vec unit]
  (cond
    (= (first mv-vec) :walk) (get-mv unit (first mv-vec))
    (= (first mv-vec) :jump) (str (get-mv unit (first mv-vec)) "j")
    :else (str (first mv-vec) " " (get-mv unit (first mv-vec)))))

(defn print-movement
  "Loops over all movements a unit has a pretty prints them."
  [unit]
  (let [mv-map (:movement unit)]
    (str/join "/" (map #(print-movement-helper % unit) mv-map))))

(defn get-hex
  [{:keys [location]}]
  (board/find-hex location))

(defn print-tmm
  [{:keys [tmm mv-hits]}]
  (loop [tmm tmm
         n 0]
    (if (= n mv-hits)
      tmm
      (recur (let [new-tmm (math/round (/ tmm 2.0))]
               (if (>= (- tmm new-tmm) 1) new-tmm 0))
             (inc n)))))

(defn tmm [{:keys [selected] :as movement} abilities]
  (let [jump-mod (or (abilities/has? abilities :jmpw) (abilities/has? abilities :jmps) 0)]
    (condp = selected
      :immobile [{:desc "Target immobile" :value -4}]
      :stand-still [{:desc "Target did not move" :value 0}]
      :jump [{:desc "Target jumped" :value (+ jump-mod (print-tmm movement) 1)}]
      [{:desc "Target moved" :value (print-tmm movement)}])))

(defn immobize [movement]
  (assoc movement :options {:immobile 0} :selected :immobile :tmm 0))

(defn take-hit [movement]
  (assoc movement :mv-hits (inc (:mv-hits movement))))

(defn amm [{:keys [selected]}]
  (condp = selected
    :immobile [{:desc "Attacker immobile" :value -1}]
    :stand-still [{:desc "Attacker stood still" :value -1}]
    :jump [{:desc "Attacker jumped" :value 2}]
    [{:desc "Attacker moved" :value 0}]))

(defn find-path
  [{:keys [selected default] :as unit} destination board]
  (board/astar (board/find-hex unit board) destination board hex/distance (or selected default)))

(defn move-costs
  [{:keys [selected default path] :as unit} board]
  (let [movement-mode (or selected default)]
    (loop [sum [(board/step-cost (board/find-hex unit board)
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
  [{:keys [path] :as unit} heat board]
  (cond
    (seq path)
    (let [sum (reduce + (move-costs unit board))
          move (get-mv unit heat)]
      (<= sum move))
    :else false))

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
        (merge unit {:location (select-keys (last (:path unit)) [:p :q :r]) :path []})
        unit))))
