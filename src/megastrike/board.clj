(ns megastrike.board
  (:require [clojure.string :as str]
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
  [line]
  (let [line-str (str/split line #" ")
        x (Integer/parseInt (subs (nth line-str 1) 0 2))
        y (Integer/parseInt (subs (nth line-str 1) 2 4))
        elevation (Integer/parseInt (nth line-str 2))
        terrain (nth line-str 3)
        style (nth line-str 4)]
    (create-tile x y elevation (strip-quotes terrain) (strip-quotes style))))

(defn create-board
  ([filename]
   (let [f (slurp filename)]
     (vec (remove nil?
                  (for [line (str/split-lines f)]
                    (when (str/includes? line "hex")
                      (parse-hex-line line)))))))
  ([width height]
   (vec (for [x (vec (range 1 (inc width))) y (vec (range 1 (inc height)))]
         (create-tile x y 0 "" "grass")))))
