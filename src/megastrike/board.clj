(ns megastrike.board
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [megastrike.hexagons.hex :as hex]
            [megastrike.utils :refer [strip-quotes]]))

(defn create-tile
  ([q r s elevation terrain palette]
   (create-tile (hex/hexagon q r s) elevation terrain palette))
  ([x y elevation terrain palette]
   (create-tile (hex/hex-from-offset x y) elevation terrain palette))
  ([hex elevation terrain palette]
   (merge hex {:elevation elevation :terrain terrain :palette palette})))

(defn parse-hex-line 
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
   (create-mapsheet filename 0 0)))

(defn create-board
  ([filename]
   (:tiles (create-mapsheet filename)))
  ([mapsheet-array width _]
     (loop [mapsheets mapsheet-array 
            hexes []
            x 0 
            y 0] 
       (if (empty? mapsheets)
         hexes
         (recur (rest mapsheets) 
                (into [] (flatten (conj hexes (:tiles (first mapsheets)))))
                (if (= (inc x) width)
                  0
                  (inc x))
                (if (= (inc x) width)
                  (inc y)
                  y)))))
  ([width height]
   (vec (for [x (vec (range 1 (inc width))) y (vec (range 1 (inc height)))]
         (create-tile x y 0 "" "grass")))))

(defn get-width
  [board]
  (first (hex/offset-from-hex (last board))))

(defn get-height
  [board]
  (second (hex/offset-from-hex (last board))))