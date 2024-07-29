(ns megastrike.combat-unit
  (:require [clojure-csv.core :as csv]
            [clojure.math :as math]
            [clojure.string :as str]
            [com.brunobonacci.mulog :as mu]
            [megastrike.utils :as utils]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map utils/keyword-maker
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(def type-schema [:enum :bm :im :pm :sv :cv :ba :ci :ss :ws :js :ds :da :sc :cf :af])
(def move-schema [:enum :walk :jump :wheeled :tracked :hover :wige :vtol])

(def all-types ["BM" "IM" "PM" "SV" "CV" "BA" "CI" "SS"
                "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def ground-units ["BM" "IM" "PM" "SV" "CV" "BA" "CI"])
(def aero-units ["SS" "WS" "JS" "DS" "DA" "SC" "CF" "AF"])
(def bm-units ["BM"])
(def mech-units ["BM" "IM" "PM"])
(def conventional-units ["SV" "CV" "BA" "CI"])
(def vehicle-units ["SV" "CV"])
(def infantry-units ["BA" "CI"])

(def directions {:n  {:angle 0 
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

(def mul-schema
  [:map 
   [:mul-id :int]
   [:chassis :string]
   [:model :string]
   [:role :string]
   [:type type-schema]
   [:size :int]
   [:movement [:map-of [keyword? int?]]]
   [:tmm :int]
   [:armor :int]
   [:structure :int]
   [:threshold :int]
   [:s :int]
   [:s* :boolean]
   [:m :int]
   [:m* :boolean]
   [:l :int]
   [:l* :boolean]
   [:e :int]
   [:e* :boolean]
   [:overheat :int]
   [:point-value :int]
   [:abilities :string]
   [:front-arc :string]
   [:left-arc :string]
   [:right-arc :string]
   [:rear-arc :string]])

(defn parse-ability 
  [str]
  (cond 
    (str/includes? "LRM" str) {:ability/type :lrm :s 0 :m 0 :l 0}
    (str/includes? "ENE" str) {:ability/type :ene}
    (str/includes? "OMNI" str) {:ability/type :omni}
    (str/includes? "JMPS" str) {:ability/type :jmps :value 0}
    (str/includes? "ECM" str) {:ability/type :ecm}
    (str/includes? "CASE" str) {:ability/type :case}
    (str/includes? "MEL" str) {:ability/type :mel}
    (str/includes? "REAR" str) {:ability/type :rear :s 0 :m 0 :l 0}
    (str/includes? "IF" str) {:ability/type :if :value 0}
    ))

(defn move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :walk
      (= mv-key (utils/keyword-maker "j")) :jump
      :else (utils/keyword-maker mv-type))))

(defn parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :jump))
      (merge mv-map {:walk (val (first mv-map))})
      mv-map)))

(defn parse-row
  "Parses a single row of the MUL file."
  ([row]
   (parse-row header-row row))
  ([hr row]
   (let [mul-row (zipmap hr row)]
     (assoc mul-row
            :full-name (str (:chassis mul-row) " " (:model mul-row))
            :mul-id (Integer/parseInt (:mul-id mul-row))
            :movement (parse-movement (:movement mul-row))
            :size (Integer/parseInt (:size mul-row))
            :tmm (Integer/parseInt (:tmm mul-row))
            :armor (Integer/parseInt (:armor mul-row))
            :structure (Integer/parseInt (:structure mul-row))
            :threshold (Integer/parseInt (:threshold mul-row))
            :s (Integer/parseInt (:s mul-row))
            :s* (if (= "TRUE" (:s* mul-row)) true false)
            :m (Integer/parseInt (:m mul-row))
            :m* (if (= "TRUE" (:m* mul-row)) true false)
            :l (Integer/parseInt (:l mul-row))
            :l* (if (= "TRUE" (:l* mul-row)) true false)
            :e (Integer/parseInt (:e mul-row))
            :e* (if (= "TRUE" (:e* mul-row)) true false)
            :overheat (Integer/parseInt (:overheat mul-row))
            :abilities (:abilities mul-row)
            :point-value (Integer/parseInt (:point-value mul-row))))))

(def mul (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-mechset-line
  "Parses a single line from a mechset file. Mechset files define which images match which units."
  [line]
  (when-not (or (= (str/index-of line "#") 0)
                (= line "")
                (= (str/index-of line "include") 0))
    (let [first-break (str/index-of line " ")
          second-break (str/index-of line "\" " (inc first-break))
          mechset-type (str/trim (subs line 0 first-break))
          search-term (str/trim (utils/strip-quotes (subs line first-break second-break)))
          file-path (str/trim (utils/strip-quotes (subs line second-break)))]
      (vector mechset-type search-term file-path))))

(defn parse-mechset
  "Parses a full Mechset file."
  []
  (into [] (remove
            nil?
            (map #(parse-mechset-line %)
                 (str/split-lines (slurp (utils/load-resource :data "images/units/mechset.txt")))))))

(def mechset (parse-mechset))

(defn filter-membership-helper
  "Returns true if a unit matches one of the types."
  ([unit]
   unit)
  ([unit field values]
   (some #(= (field unit) %) values)))

(defn filter-units
  "Filters units based on either a string or a seq of unit type."
  ([units]
   units)
  ([units field value comparison]
   (filter #(when (comparison (field %) value) %) units))
  ([units field values]
   (filter #(filter-membership-helper % field values) units)))

(defn get-unit
  ([s]
   (let [non-standard (str/replace s #"\(Standard\)" "")
         matching-muls (filter-units mul :full-name s =)
         non-standard-mul (filter-units mul :full-name non-standard =)] 
     (mu/log ::get-unit-function
             :search-term s
             :matching-mul-results matching-muls
             :non-standard-results non-standard-mul)
     (if (first matching-muls)
       (first matching-muls)
       (first non-standard-mul)))))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data))
  ([units mul-unit game-data]
   (let [matching-units (filter (fn [x] (when (and (:id x) (:full-name mul-unit)) 
                                          (str/includes? (:id x) (:full-name mul-unit)))) (vals units))
         id (if (seq matching-units)
              (str (:full-name mul-unit) " #" (inc (count matching-units)))
              (str (:full-name mul-unit)))
         unit (merge mul-unit {:id id} game-data)]
     (mu/log ::element-created 
             :element unit)
     (merge units {id unit}))))

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [{:keys [chassis full-name]}]
  (let [chassis-match (filter (fn [row] (= (second row) chassis)) mechset)
        exact-match (filter (fn [row] (str/includes? (second row) full-name)) mechset)
        match-row (or (first exact-match) (first chassis-match))] 
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

(defn get-mv
  ([unit move-type]
   (let [move (get-in unit [:movement move-type])
         div (count (filter #(= :mv %) (:crits unit)))] 
     (loop [mv move
            n 0]
       (if (= n div)
         (max (- mv (:current-heat unit 0)) 0)
         (recur (let [new-mv (math/round (/ mv 2.0))]
                  (if (>= (- mv new-mv) 1) new-mv 0))
                (inc n))))))
  ([unit]
  (get-mv unit (get unit :movement :walk))))

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

(defn pv-mod
  "Calculates the skill-based mod for PV based on the algorithm provided in the book."
  [{:keys [pilot point-value]}]
  (let [skill-diff (- 4 (:skill pilot))]
    (cond
      (> 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- point-value 5) 10)))
      (< 0 skill-diff) (* skill-diff (+ 1 (math/floor-div (- point-value 3) 5)))
      :else 0)))

(defn pv
  "Returns the modified PV."
  [{:keys [point-value] :as unit}]
  (+ point-value (pv-mod unit)))

(defn print-short
  [{:keys [s s*]}]
  (if s*
    "0*"
    (str s)))

(defn print-medium
  [{:keys [m m*]}]
  (if m*
    "0*"
    (str m)))

(defn print-long
  [{:keys [l l*]}]
  (if l*
    "0*"
    (str l)))

(defn print-extreme
  [{:keys [e e*]}]
  (if e*
    "0*"
    (str e)))

(defn print-damage
  [{:keys [size abilities] :as unit} range physical]
  (cond 
     (and (= range 1) physical) (+ size (if (str/includes? abilities "MEL") 1 0))
     (>= 3 range) (print-short unit)
     (>= 12 range) (print-medium unit)
     (>= 21 range) (print-long unit)
     (>= 30 range) (print-extreme unit)
     :else 0))

(defn calculate-damage
  "Returns the damage done by a unit at a given range. Calculates 0* damage correctly."
  [{:keys [attack size abilities s s* m m* l l* e e*]} range rear-attack?]
   (let [damage (cond
                  (and (= range 1) (= attack :physical)) (+ size (if (str/includes? abilities "MEL") 1 0))
                  (>= 3 range) (if (and  s* (<= 4 (utils/roll-die))) 1 s)
                  (>= 12 range) (if (and m* (<= 4 (utils/roll-die))) 1 m)
                  (>= 21 range) (if (and l* (<= 4 (utils/roll-die))) 1 l)
                  (>= 30 range) (if (and e* (<= 4 (utils/roll-die))) 1 e)
                  :else 0)]
     (if rear-attack?
       (inc damage)
       damage)))

(defn take-weapon-hit 
  [unit]
  (assoc unit 
         :s (max (dec (:s unit)) 0) 
         :s* false
         :m (max (dec (:m unit)) 0)
         :m* false
         :l (max (dec (:l unit)) 0)
         :l* false
         :e (max (dec (:e unit)) 0)
         :e* false
         :crits (conj (:crits unit) :weapon)))
