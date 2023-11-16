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
  [hr row]
  (let [mul-row (zipmap hr row)]
    (assoc mul-row
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
           :point-value (Integer/parseInt (:point-value mul-row)))))

