(ns megastrike.combat-unit
  (:require [clojure-csv.core :as csv]
            [clojure.string :as string]
            [megastrike.utils :refer [keyword-maker]]))

(def header-row
  "Defines the header row which will serve as the keys for the creation of combat units."
  (map keyword-maker
       (first (csv/parse-csv (slurp "resources/mul.csv") :delimiter \tab))))

(defn move-keyword
  [mv-type]
  (let [mv-key (keyword-maker mv-type)]
    (cond
      (= mv-key (keyword-maker "")) :walk
      (= mv-key (keyword-maker "j")) :jump
      :else (keyword-maker mv-type))))

(defn parse-movement
  [mv-string]
  (let [strings (re-seq #"(\d+)\\+\"([a-zA-Z]?)" mv-string)]
    (into {} (map #(vector (move-keyword (nth % 2)) (/ (Integer/parseInt (second %)) 2)) strings))))

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

(defn filter-by-name
  ([units]
   units)
  ([units name]
  (filter #(if (= (:full-name %) name) %) units)))

(defn filter-by-pv
  ([units]
   units)
  ([units value comparison]
   (filter #(if (apply comparison (:point-value %) value) %) units)))

(defn filter-units
  ([units]
   units)
  ([units field value comparison]
   (filter #(if (comparison (field %) value) %) units)))

(defn create-element
  "Creates an element for use in the game."
  ([mul-unit game-data]
   (merge mul-unit game-data)))

(defn get-unit
  [li]
  (first li))
