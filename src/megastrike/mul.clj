(ns megastrike.mul
  (:require
   [clojure-csv.core :as csv]
   [clojure.string :as str]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.attack :as attack]
   [megastrike.combat :as combat]
   [megastrike.heat :as heat]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map utils/keyword-maker
       (first (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

(def all-types #{"BM" "IM" "PM" "SV" "CV" "BA" "CI" "SS"
                 "WS" "JS" "DS" "DA" "SC" "CF" "AF"})
(def ground-units #{"BM" "IM" "PM" "SV" "CV" "BA" "CI"})
(def aero-units #{"SS" "WS" "JS" "DS" "DA" "SC" "CF" "AF"})
(def bm-units #{"BM"})
(def mech-units #{"BM" "IM" "PM"})
(def conventional-units #{"SV" "CV" "BA" "CI"})
(def vehicle-units #{"SV" "CV"})
(def infantry-units #{"BA" "CI"})

(defn parse-row
  "Parses a single row of the MUL file."
  ([row]
   (parse-row header-row row))
  ([hr row]
   (let [mul-row (zipmap hr row)
         movement (movement/->movement mul-row)
         abilities (abilities/parse-abilities (:abilities mul-row))]
     (assoc mul-row
            :full-name (str (:chassis mul-row) " " (:model mul-row))
            :mul-id (Integer/parseInt (:mul-id mul-row))
            :movement (movement/->movement mul-row)
            :size (Integer/parseInt (:size mul-row))
            :attacks (attack/->attacks mul-row movement abilities)
            :combat (combat/->combat mul-row)
            :heat (heat/->heat 0 (Integer/parseInt (:overheat mul-row)))
            :abilities abilities
            :point-value (Integer/parseInt (:point-value mul-row))))))

(def mul
  (map parse-row (rest (csv/parse-csv (slurp (utils/load-resource :resources "mul.csv")) :delimiter \tab))))

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

(defn find-sprite
  "Searches a the mechset to determine which images to use and returns the path to that image."
  [{:keys [chassis full-name]}]
  (let [chassis-match (filter (fn [row] (= (second row) chassis)) mechset)
        exact-match (filter (fn [row] (str/includes? (second row) full-name)) mechset)
        match-row (or (first exact-match) (first chassis-match))]
    (utils/load-resource :data (str "images/units/" (nth match-row 2)))))

(defn print-abilities
  [{:keys [abilities]}]
  (->> abilities
       (vals)
       (map :output)
       (str/join ", ")))

