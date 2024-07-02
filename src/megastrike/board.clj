(ns megastrike.board
  (:require [clojure.data.priority-map :as priority-map]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [megastrike.hexagons.hex :as hex]
            [megastrike.utils :refer [strip-quotes]]))

(defn create-tile
  "Returns a map with the {:q :r :s} address as the key and the full hex map 
   (with the address + elevation, terrain, palette, etc) as the value."
  ([q r s elevation terrain palette]
   (create-tile (hex/hexagon q r s) elevation terrain palette))
  ([x y elevation terrain palette]
   (create-tile (hex/hex-from-offset x y) elevation terrain palette))
  ([hex elevation terrain palette]
   (merge hex {:elevation elevation :terrain terrain :palette palette})))

(defn parse-hex-line 
  "Parses a line from a .board file and returns a map created by `create-tile`."
  ([line x-offset y-offset]
   (let [line-str (str/split line #" ")
         x (+ (Integer/parseInt (subs (nth line-str 1) 0 2)) x-offset)
         y (+ (Integer/parseInt (subs (nth line-str 1) 2 4)) y-offset)
         elevation (Integer/parseInt (nth line-str 2))
         terrain (nth line-str 3)
         style (nth line-str 4)]
     (create-tile x y elevation (strip-quotes terrain) (strip-quotes style))))
  ([line] 
   (parse-hex-line line 0 0)))

;; This area needs to be refactored and thought about to unify my vision for how this data is stored.
;; Mapsheets are a collection of tiles
;; Boards are a collection of mapsheets. But also need to be a collection of tiles in order to make display easier

;; Mapsheets should have
;; :name - The name of the mapsheet, for display purposes in the lobby
;; :width - Number of hexes wide
;; :height - Number of hexes tall
;; :tiles - The vector of tiles that make up the mapsheet
;; :mapsheets - Could just return itself?
;; (def mapsheet {:name "16x17 Desert Canyon.board" :width 16 :height 17 :tiles [vector-of-tile-maps]})

;; Boards should have
;; :tiles - The vector of tiles that make up the board (or a function that returns the tiles from the mapsheets)
;; :mapsheets - a 2D vector of all the mapsheets
;; (def board {:tiles (flatten (map #(:tiles %) (:mapsheets board)) :mapsheets [[mapsheet-0-0 mapsheet-1-0] [mapsheet-0-1 mapsheet-1-1]])})

(defn create-mapsheet 
  ([filename x-offset y-offset]
   (loop [mapsheet {:name (.getName (io/file filename)) :height 0 :width 0 :tiles []}
          lines (str/split-lines (slurp filename))]
     (if (empty? lines)
         mapsheet
         (recur (let [line (first lines)]
                  (cond 
                    (str/includes? line "size") (merge mapsheet {:width (Integer/parseInt (second (str/split line #" "))) 
                                                                          :height (Integer/parseInt (nth (str/split line #" ") 2))})
                    (str/includes? line "hex") (assoc mapsheet :tiles (conj (:tiles mapsheet) (parse-hex-line line x-offset y-offset)))
                    :else mapsheet))
                (rest lines)))))
  ([filename]
   (create-mapsheet filename 0 0))
  ([width height]
   {:name "Generated" :height height :width width :tiles (into [] (for [x (range 1 (inc width))
                                                                        y (range 1 (inc height))]
                                                                    (create-tile x y 0 "" "grass")))}))

(defprotocol BOARD
  (nodes [board])
  (neighbors [board node])
  (mapsheets [board])
  (weight [board from to mv-type]))

(defn create-board
  ([filename]
   (let [mapsheet (create-mapsheet filename)]
     (reify BOARD
       (nodes [_] (:tiles mapsheet))
       (neighbors [_ node] (into [] (remove nil? (map #(hex/find-hex % (:tiles mapsheet)) (hex/hex-neighbors node)))))
       (mapsheets [_] [[mapsheet]])
       (weight [_ from to mv-type] (hex/step-cost from to mv-type)))))
  ([mapsheet-array _ _]
     (let [tiles ((comp vec flatten vector) (for [m mapsheet-array] (:tiles m nil)))]
       (reify BOARD
        (nodes [_] tiles)
        (neighbors [_ node] (into [] (remove nil? (map #(hex/find-hex % tiles) (hex/hex-neighbors node)))))
        (mapsheets [_] mapsheet-array)
        (weight [_ from to mv-type] (hex/step-cost from to mv-type)))))
  ([width height]
   (let [mapsheet (create-mapsheet width height)]
     (reify BOARD
       (nodes [_] (:tiles mapsheet))
       (neighbors [_ node] (into [] (remove nil? (map #(hex/find-hex % (:tiles mapsheet)) (hex/hex-neighbors node)))))
       (mapsheets [_] [[mapsheet]])
       (weight [_ from to mv-type] (hex/step-cost from to mv-type))))))

(defn- calc-approx-dist [h dist]
  (for [[node d] dist] 
    [node (+ d (h node))]))

(defn astar
  [start goal graph heuristic mv-type]
  (let [guess-goal-dist #(heuristic % goal)
        nodes (nodes graph)
        neighbors #(neighbors graph %)
        weight #(weight graph %1 %2 mv-type)]
    (loop [known-dist (merge (into {} (for [x nodes] [x ##Inf]))
                             {start 0})
           guess-unseen-dist (into (priority-map/priority-map)
                                   (calc-approx-dist guess-goal-dist known-dist))
           visited? #{}
           path {start []}]
      (let [[best-unseen _] (peek guess-unseen-dist)] 
        (if (or (= (known-dist best-unseen) ##Inf)
                (visited? goal)
                (empty? guess-unseen-dist))
          (if goal (path goal) path)
          (let [closer-nbrs (for [nbr (neighbors best-unseen) 
                                  :let [new-known-dist (+ (known-dist best-unseen) 
                                                          (weight best-unseen nbr))] 
                                  :when (< new-known-dist (known-dist nbr))]
                              [nbr new-known-dist])
                closer-paths (for [[nbr _ ] closer-nbrs]
                               [nbr (conj (path best-unseen) nbr)])] 
            (recur (into known-dist closer-nbrs)
                   (into (pop guess-unseen-dist)
                         (calc-approx-dist guess-goal-dist closer-nbrs))
                   (conj visited? best-unseen)
                   (into path closer-paths))))))))
