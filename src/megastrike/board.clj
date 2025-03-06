(ns megastrike.board
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :refer [strip-quotes]]))

(defn create-tile
  "Returns a map with the {:q :r :s} address as the key and the full hex map 
   (with the address + elevation, terrain, palette, etc) as the value."
  ([q r s elevation terrain palette]
   (create-tile (hex/hexagon q r s) elevation terrain palette))
  ([x y elevation terrain palette]
   (create-tile (hex/offset->hex x y) elevation terrain palette))
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
   {:name "Generated"
    :height height
    :width width
    :tiles (into [] (for [x (range 1 (inc width))
                          y (range 1 (inc height))]
                      (create-tile x y 0 "" "grass")))}))

;; Why is a protocol required here, can I get by without one?

(defn tiles
  [board]
  (:tiles board))

(defn find-hex
  [h board]
  (let [found (first (filter #(hex/same-hex h %) (tiles board)))]
    found))

(defn update-hex
  [new-h board]
  (assoc board :tiles (for [h (tiles board)]
                        (if (hex/same-hex h new-h)
                          new-h
                          h))))

(defn set-stacking
  "Mark all units on the board."
  [board units]
  (loop [board board
         units units]
    (if (empty? units)
      board
      (recur (let [u (first units)
                   loc (first u)
                   force (second u)
                   tile (assoc (find-hex loc board) :stacking force)
                   upd (update-hex tile board)]
               upd)
             (rest units)))))

(defn linear-interpolation
  [a b step]
  (+ a (* (- b a) step)))

(defn lerp
  [hex1 hex2 step]
  {:p (linear-interpolation (:p hex1) (:p hex2) step)
   :q (linear-interpolation (:q hex1) (:q hex2) step)
   :r (linear-interpolation (:r hex1) (:r hex2) step)})

(defn line
  [hex1 hex2 board]
  (let [distance (hex/distance hex1 hex2)]
    (loop [result []
           step 0]
      (if (= step distance)
        (conj result (find-hex hex2 board))
        (recur (conj result (find-hex (hex/round (lerp hex1 hex2 (* (/ 1.0 distance) step))) board))
               (inc step))))))

(defn step-cost
  [hex neighbor mv-type unit-force]
  (let [terrain (get neighbor :terrain "")
        lvl-change (- (get neighbor :elevation 0) (get hex :elevation 0))]
    (try
      (str/includes? terrain "woods")
      (catch Exception e
        (mu/log ::step-cost-failed
                :error e
                :hex hex
                :neighbor neighbor
                :mv-type mv-type)))
    (cond
      (= mv-type :jump) 1
      (and (get neighbor :stacking false) (not= (get neighbor :stacking false) unit-force)) ##Inf
      (> lvl-change 2) ##Inf
      (str/includes? terrain "woods") (+ (abs lvl-change) 2)
      (str/includes? terrain "rough") (+ (abs lvl-change) 2)
      (str/includes? terrain "rubble") (+ (abs lvl-change) 2)
      (str/includes? terrain "water") (+ (abs lvl-change) 2)
      :else (+ (abs lvl-change) 1))))

(defn path-cost
  [path mv-type unit-force]
  (loop [start (first path)
         path (rest path)
         result []]
    (if (= (count path) 0)
      result
      (recur (first path)
             (rest path)
             (conj result (step-cost start (first path) mv-type unit-force))))))

(defn neighbors
  [board node]
  (let [return (map #(find-hex % board) (hex/neighbors node))]
    (into [] (remove nil? return))))

(defn create-board
  ([filename]
   (let [mapsheet (create-mapsheet filename)]
     {:tiles (:tiles mapsheet)
      :mapsheets [[mapsheet]]}))
  ([mapsheet-array _ _]
   (let [tiles ((comp vec flatten vector) (for [m mapsheet-array] (:tiles m nil)))]
     {:tiles tiles
      :mapsheets mapsheet-array}))
  ([width height]
   (let [mapsheet (create-mapsheet width height)]
     {:tiles (:tiles mapsheet)
      :mapsheets [[mapsheet]]})))
