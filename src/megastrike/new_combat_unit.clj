(ns megastrike.new-combat-unit
  (:require
   [clojure-csv.core :as csv]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [megastrike.abilities :as abilities]
   [megastrike.attacks :as attacks]
   [megastrike.damage :as damage]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(defn- move-keyword
  "Creates a move keyword from a stat line imported from the mul export."
  [mv-type]
  (let [mv-key (utils/keyword-maker mv-type)]
    (cond
      (= mv-key (utils/keyword-maker "")) :move/walk
      (= mv-key (utils/keyword-maker "j")) :move/jump
      :else (keyword "move" (-> mv-type
                                (string/trim)
                                (string/lower-case)
                                (utils/remove-parens)
                                (utils/correct-range-brackets)
                                (utils/replace-spaces))))))

(defn- parse-movement
  "Parses a string like 8\"/5\"j into a map of all the possible movement modes the unit has and their distance in hexes."
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)
        mv-map (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))]
    (if (and (= (count mv-map) 1) (= (key (first mv-map)) :move/jump))
      (merge mv-map {:move/walk (val (first mv-map))})
      mv-map)))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map utils/keyword-maker
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn parse-row
  "Parses a single row of the MUL file."
  ([row]
   (parse-row header-row row))
  ([hr row]
   (let [mul-row (zipmap hr row)
         modes (parse-movement (:movement mul-row))
         movement (movement/create-movement mul-row)
         abilities (abilities/parse-abilities (:abilities mul-row))]
     (s/assert :unit/mul
               {:unit/chassis (:chassis mul-row)
                :unit/model (:model mul-row)
                :unit/role (utils/keyword-maker (:role mul-row))
                :unit/type (utils/keyword-maker (:type mul-row))
                :unit/threshold (Integer/parseInt (:threshold mul-row))
                :unit/full-name (str (:chassis mul-row) " " (:model mul-row))
                :unit/mul-id (Integer/parseInt (:mul-id mul-row))
                :unit/size (Integer/parseInt (:size mul-row))
                :unit/move-modes modes
                :unit/structure {:toughness/current (Integer/parseInt (:armor mul-row))
                                 :toughness/maximum (Integer/parseInt (:armor mul-row))
                                 :toughness/unapplied 0}
                :unit/armor {:toughness/current (Integer/parseInt (:armor mul-row))
                             :toughness/maximum (Integer/parseInt (:armor mul-row))
                             :toughness/unapplied 0}
                :unit/tmm (Integer/parseInt (:tmm mul-row))
                :unit/movement movement
                :unit/attacks (attacks/->attacks mul-row movement abilities)
                :unit/damage (damage/->damage mul-row)
                :unit/overheat (Integer/parseInt (:overheat mul-row))
                :unit/abilities abilities
                :unit/base-pv (Integer/parseInt (:point-value mul-row))}))))

(def mul
  (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(defn ->combat-unit
  [mul-unit pilot facing location battle-force number]
  (merge mul-unit
         {:unit/id (if (pos? number)
                     (str (:unit/full-name mul-unit) " #" (inc number))
                     (:full-name mul-unit))
          :unit/battle-force battle-force
          :unit/pilot pilot
          :unit/facing facing
          :unit/location location
          :unit/criticals {:crits/taken [] :crits/unapplied []}
          :unit/selected nil
          :unit/default (if (contains? (:unit/move-modes mul-unit) :move/walk) :move/walk (first (keys (:unit/move-modes mul-unit))))}))

(defn parse-mechset-line
  "Parses a single line from a mechset file. Mechset files define which images match which units."
  [line]
  (when-not (or (= (string/index-of line "#") 0)
                (= line "")
                (= (string/index-of line "include") 0))
    (let [first-break (string/index-of line " ")
          second-break (string/index-of line "\" " (inc first-break))
          mechset-type (string/trim (subs line 0 first-break))
          search-term (string/trim (utils/strip-quotes (subs line first-break second-break)))
          file-path (string/trim (utils/strip-quotes (subs line second-break)))]
      (vector mechset-type search-term file-path))))

(defn parse-mechset
  "Parses a full Mechset file."
  []
  (into [] (remove
            nil?
            (map #(parse-mechset-line %)
                 (string/split-lines (slurp (utils/load-resource :data "images/units/mechset.txt")))))))

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
   (let [non-standard (string/replace s #"\(Standard\)" "")
         matching-muls (filter-units mul :full-name s =)

         non-standard-mul (filter-units mul :full-name non-standard =)]
     (if (first matching-muls)
       (first matching-muls)
       (first non-standard-mul)))))

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [{:keys [chassis full-name]}]
  (let [chassis-match (filter (fn [row] (= (second row) chassis)) mechset)
        exact-match (filter (fn [row] (string/includes? (second row) full-name)) mechset)
        match-row (or (first exact-match) (first chassis-match))]
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

(defn print-abilities
  [{:keys [abilities]}]
  (->> abilities
       (vals)
       (map :output)
       (string/join ", ")))

