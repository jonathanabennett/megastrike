(ns megastrike.scenario
  (:require
   [clojure.java.io :as io]
   [clojure.math :as math]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.battle-force :as battle-force]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]
   [megastrike.utils :as utils]))

(defn edn-scenario-parser
  [file]
  ;; Verify file type is correct
  ;; Parse mapsheets
  ;; Parse battleforces
  ;; Parse units
  ;; Return state
  file)

(defn write-forces
  [forces]
  (mapv #(assoc % :unit-group/camo (utils/relative-path (:unit-group/camo %))) forces))

(defn write-mapsheets
  [sheets]
  (mapv :name (:mapsheets sheets)))

(defn units-writer
  [units]
  (mapv #(assoc % :unit/sprite (utils/relative-path (:unit/sprite %))) units))

(defn edn-scenario-writer
  [state]
  (-> state
      (assoc :forces (write-forces (:forces state)))
      (assoc :maps (write-mapsheets (:game-board state)))
      (dissoc :game-board)
      (assoc :units  (units-writer (:units state)))))

(defn initialize-forces
  [forces]
  (loop [force-vec []
         i 1
         names (str/split forces #",")]
    (if (empty? names)
      force-vec
      (recur (conj force-vec (battle-force/->battle-force (first names) nil nil i :player []))
             (inc i)
             (rest names)))))

(defn extract-name [input]
  (let [pattern #"_(.*?)="]
    (keyword (utils/keyword-maker (str (second (re-find pattern input)))))))

(defn set-location
  [state line value]
  (let [force-name (extract-name line)]
    (assoc state
           :forces
           (battle-force/update-battle-force
            (:forces state)
            (keyword force-name)
            :unit-group/deployment
            (keyword "direction" (utils/keyword-maker value))))))

(defn set-team
  [state line value]
  (let [force-name (extract-name line)]
    (assoc state
           :forces
           (battle-force/update-battle-force
            (:forces state)
            (keyword force-name)
            :unit-group/deployment
            (Integer/parseInt value)))))

(defn set-camo
  [state line value]
  (let [force-name (extract-name line)]
    (assoc state
           :forces
           (battle-force/update-battle-force
            (:forces state)
            (keyword force-name)
            :unit-group/camo
            value))))

(defn parse-unit-string [s]
  (let [[_ faction number data] (re-matches #"Unit_(\w+)_(\d+)[=_](.+)" s)]
    [faction (Integer/parseInt number) data]))

(defn configure-unit
  [state line]
  (if (re-find #"\d+=" line)
    (let [units (get state :units [])
          [faction _ data] (parse-unit-string line)
          [unit pilot pskill gskill direction x y] (str/split data #",")
          loc (if (and x y) (hex/offset->hex (Integer/parseInt (str/trim x)) (Integer/parseInt (str/trim y))) {})
          skill (int (math/floor (/ (+ (Integer/parseInt pskill) (Integer/parseInt gskill)) 2)))
          mul (cu/->combat-unit {:units units
                                 :mul-unit (cu/get-unit unit)
                                 :pilot {:pilot/full-name pilot :pilot/skill skill :pilot/kills 0}
                                 :battle-force (keyword (utils/keyword-maker faction))
                                 :facing (keyword "direction" (if direction (utils/keyword-maker direction) "n"))
                                 :location loc})]
      mul)

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
      (str/starts-with? line "Location") (set-location state line value)
      (str/starts-with? line "Team") (set-team state line value)
      (str/includes? line "Camo") (set-camo state line value)
      (str/includes? line "Damage") state
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
  (let [size-string (str (:width map-size) "x" (:height map-size))
        map-list (filter #(str/includes? (str (.getName %)) size-string) board-files)
        width (* (first loc) (:width map-size))
        height (* (second loc) (:height map-size))]
    (board/create-mapsheet (str "file:" (.getPath (rand-nth map-list))) width height)))

(defn map-rotator
  [filename]
  (let [f (if (str/includes? filename "rotate:")
            {:board (utils/load-resource :data (str "boards/" (second (str/split filename #":")) ".board")) :rotate true}
            {:board (utils/load-resource :data (str "boards/" filename ".board")) :rotate nil})]
    {:original f :temp (board/create-mapsheet (:board f))}))

(defn map-processor
  [filename]
  (board/create-mapsheet (str "file:" (.getPath filename))))

(defn set-maps
  [{:keys [map-dirs map-width map-height maps]}]
  (if map-dirs
    (let [boards (board-files map-dirs)
          size-setter (map-processor (rand-nth boards))
          maps (for [x (range map-width)
                     y (range map-height)]
                 [x y])]
      {:map-boards (into [] (map #(pick-map % boards size-setter) maps))})

    (do (prn maps)
        (let [maps (map #(map-rotator (str/trim %)) maps)
              width (get-in (first maps) [:temp :width])
              height (get-in (first maps) [:temp :height])
              offsets (for [x (range map-width)
                            y (range map-height)]
                        [(* width x)
                         (* height y)])]
          (loop [ret []
                 n 0]
            (if (= (count maps) n)
              {:map-boards ret}
              (recur (conj ret (board/create-mapsheet (get-in (nth maps n) [:original :board]) (first (nth offsets n)) (second (nth offsets n))))
                     (inc n))))))))

(defn parse-scenario-file
  [file]
  (loop [state {:units []}
         f (str/split-lines (slurp (io/file file)))]
    (if (empty? f)
      state
      (recur (parse-line state (first f))
             (rest f)))))

(defn server-parse-scenario
  [data]
  (loop [state {:units []}
         f (str/split-lines data)]
    (if (empty? f)
      state
      (do (prn f)
          (recur (parse-line state (first f))
                 (rest f))))))

(defn server-setup-scenario
  [data]
  (let [scenario (server-parse-scenario data)
        map-layout (set-maps scenario)]
    {:lobby map-layout :game (merge scenario {:map-width (:map-width scenario) :map-height (:map-height scenario)})}))

(defn setup-scenario
  [file]
  (let [scenario (parse-scenario-file file)
        map-layout (set-maps scenario)]
    {:lobby map-layout :game (merge scenario {:map-width (:map-width scenario) :map-height (:map-height scenario)})}))
