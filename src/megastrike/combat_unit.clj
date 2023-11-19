(ns megastrike.combat-unit
  (:require [clojure-csv.core :as csv]
            [clojure.string :as string]))

(defn keyword-maker
  "Take a string with spaces, strips them out, and turns it into a keyword"
  [str]
  (keyword (string/lower-case (string/replace str " " "-"))))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map keyword-maker
       (first (csv/parse-csv
               (slurp "resources/mul.csv")
               :delimiter \tab))))

(defn parse-movement
  [mv-string]
  (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string))

(defn parse-row
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
           :point-value (Integer/parseInt (:point-value mul-row))))))

(def mul (map parse-row (rest (csv/parse-csv (slurp "resources/mul.csv") :delimiter \tab))))

(defn filter-units
  "Applies a filter to a list of units."
  ([li]
   li)
  ([filter-fn li]
   (filter filter-fn li)))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data))
  ([unit force move-info pv-mod current-armor
    current-structure crits destroyed? target
    current-heat acted? pilot]
   (create-element unit {:force force :move-info move-info
                         :pv-mod pv-mod :current-armor current-armor
                         :current-structure current-structure
                         :crits crits :destroyed? destroyed?
                         :target target :current-heat current-heat
                         :acted? acted? :pilot pilot})))

(comment
  (filter #(if (= (:type %) "BM") %) mul)
  )
