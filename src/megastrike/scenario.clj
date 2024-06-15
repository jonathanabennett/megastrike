(ns megastrike.scenario
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.math :as math]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]
            [megastrike.utils :as utils]))

(defn initialize-forces
  [forces]
  (loop [force-map {}
         names (str/split forces #",")]
    (if (empty? names)
      force-map
      (recur (merge force-map {(utils/keyword-maker (first names)) {:name (first names)}})
             (rest names)))))

(defn extract-name [input]
  (let [pattern #"_(.*?)="]
    (utils/keyword-maker (str (second (re-find pattern input))))))

(defn update-force
  [state line key value]
  (let [force-name (extract-name line)
        force (merge (get (:forces state) force-name) {key value})
        new-forces (assoc (:forces state) force-name force)]
    (assoc state :forces new-forces)))

(defn set-location
  [state line value]
  (update-force state line :location value))

(defn set-team
  [state line value]
  (update-force state line :team (Integer/parseInt value)))

(defn set-camo
  [state line value]
  (update-force state line :camo value))

(defn parse-unit-string [s]
  (let [[_ faction number data] (re-matches #"Unit_(\w+)_(\d+)[=_](.+)" s)]
      [faction (Integer/parseInt number) data]))

(defn configure-unit
  [state line]
  (if (re-find #"\d+=" line)
    (let [[faction num data] (parse-unit-string line)
          [unit pilot pskill gskill direction x y] (str/split data #",")
          loc (if (and x y) (hex/hex-from-offset (Integer/parseInt (str/trim x)) (Integer/parseInt (str/trim y))) {})
          skill (int (math/floor (/ (+ (Integer/parseInt pskill) (Integer/parseInt gskill)) 2)))
          mul (first (cu/filter-units cu/mul :full-name unit str/includes?))] 
      (cu/create-element (get state :units {}) 
                         mul
                         (merge loc {:force (utils/keyword-maker faction) 
                                     :pilot 
                                     {:name pilot 
                                      :skill skill} 
                                     :current-armor (:armor mul) 
                                     :current-structure (:structure mul) 
                                     :current-heat 0})))
    (:units state)))

(defn set-map-dirs
  [line]
  (let [dirs (str/split line #",")]
    (for [d dirs] (io/file (str utils/application-directory "/data/boards/" d)))))


(defn parse-line 
  [state line]
  (let [value (second (str/split line #"="))] 
    (cond 
      (= (str/index-of line "#") 0) state 
      (str/includes? line "BoardWidth") (merge state {:map-width (Integer/parseInt value)})
      (str/includes? line "BoardHeight") (merge state {:map-height (Integer/parseInt value)})
      (str/includes? line "RandomDirs") (merge state {:map-dirs (set-map-dirs value)})
      (str/includes? line "Maps") (merge state {:maps (str/split value #",")})
      (str/includes? line "Factions") (merge state {:forces (initialize-forces value)})
      (str/includes? line "Location") (set-location state line value)
      (str/includes? line "Team") (set-team state line value) 
      (str/includes? line "Camo") (set-camo state line value) 
      (str/includes? line "Unit") (assoc state :units (configure-unit state line)) 
      :else state)))
;; Use helper methods where if we see "Camo" or "Unit" or the other player
;; specific indicators, we kick out to a helper method to parse that line
;; based on the data in state.

(defn board-files [dirs]
  (let [is-board-file? #(-> % .getName (str/ends-with? ".board"))
        board-files-in-dir (fn [dir]
                             (filter is-board-file? (file-seq (io/file dir))))
        boards (vec (mapcat board-files-in-dir dirs))]
    boards))

(defn pick-map
  [loc board-files map-size]
  (let [map-list (filter #(str/includes? (str (.getName %)) map-size) board-files)
        width (* (first loc) (Integer/parseInt (first (str/split map-size #"x"))))
        height (* (second loc) (Integer/parseInt (second (str/split map-size #"x"))))]
    (board/create-mapsheet (str "file:" (.getPath (rand-nth map-list))) width height)))

(defn map-rotator
  [filename]
  (if (str/includes? filename "rotate:")
    {:board (utils/load-resource :data (str "boards/" (second (str/split filename #":")) ".board")) :rotate true}
    {:board (utils/load-resource :data (str "boards/" filename ".board")) :rotate nil}))

(defn set-maps
  [scenario]
  (if (:map-dirs scenario)
    (let [boards (board-files (:map-dirs scenario))
          map-size (first (str/split (.getName (rand-nth boards)) #" "))
          maps (for [x (range (:map-width scenario)) 
                     y (range (:map-height scenario))] 
                 [x y])]
      {:map-boards (into [] (map #(pick-map % boards map-size) maps))}) 
    (let [maps (map #(map-rotator %) (:maps scenario)) 
          size-string (first (str/split (.getName (:board (first maps))) #" ")) 
          width (Integer/parseInt (first (str/split size-string #"x")))
          height (Integer/parseInt (second (str/split size-string #"x")))
          offsets (for [x (range (:map-width scenario))
                        y (range (:map-height scenario))]
                    [(* width x) 
                     (* height y)])] 
      (loop [ret []
             n 0]
        (if (= (count maps) n)
          {:map-boards ret}
          (let [test 0]
            (prn (nth maps n))
            (prn (nth offsets n))
            (recur (conj ret (board/create-mapsheet (:board (nth maps n)) (first (nth offsets n)) (second (nth offsets n))))
                   (inc n))))))))

(defn parse-scenario-file
  [file]
  (loop [state {}
         f (str/split-lines (slurp (io/file file)))] 
    (if (empty? f)
      state
      (recur (parse-line state (first f))
             (rest f)))))

(defn setup-scenario
  [file]
  (let [scenario (parse-scenario-file file)
        map-layout (set-maps scenario)] 
    ;;(pprint/pprint map-layout)
    (merge scenario map-layout {:map-width (str (:map-width scenario)) :map-height (str (:map-height scenario))})))