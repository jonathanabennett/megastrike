(ns megastrike.scenario
  (:require [clojure.string :as str]
            [megastrike.combat-unit :as cu]
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
    (utils/keyword-maker (re-find pattern input))))

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
  (let [[faction num data] (parse-unit-string line)
        [unit pilot pskill gskill direction x y] (str/split data #",")
        mul (first (cu/filter-units cu/mul :full-name unit str/includes?))]
    (prn (utils/keyword-maker faction))
    (prn mul)
    (prn pilot)
    (prn pskill)
    (prn gskill)
    (prn direction)
    (prn x)
    (prn y)
    ))

(defn parse-line 
  [line state]
  (let [value (second (str/split line #"="))]
    (cond 
      (= (str/index-of line "#") 0) state 
      (str/includes? line "BoardWidth") (merge state {:board-width (Integer/parseInt value)})
      (str/includes? line "BoardHeight") (merge state {:board-height (Integer/parseInt value)})
      (str/includes? line "RandomDirs") (merge state {:map-dirs value})
      (str/includes? line "Maps") (merge state {:maps value})
      (str/includes? line "Factions") (merge state {:forces (initialize-forces value)})
      (str/includes? line "Location") (set-location state line value)
      (str/includes? line "Team") (set-team state line value) 
      (str/includes? line "Camo") (set-camo state line value) 
      (str/includes? line "Unit") (configure-unit state line) :else state)))
;; Use helper methods where if we see "Camo" or "Unit" or the other player
;; specific indicators, we kick out to a helper method to parse that line
;; based on the data in state.