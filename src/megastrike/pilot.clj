(ns megastrike.pilot
  "Handles pilots and crews for units.
  
  `->pilot` creates a pilot from a name and skill
  `skill` gets the pilot's skill value as an int
  `fullname` gets the pilot's name
  `display` prints the pilot's stats as name(skill), so 'Bob Kim(4)'"
  (:require
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema and constructor
(defn valid-pilot-skill?
  [n]
  (<= 0 n 8))

(defn ->pilot
  ([pname skill]
   (let [pname (string/trim pname)
         skill (if (= (type skill) java.lang.String)
                 (Integer/parseInt skill)
                 skill)
         pilot {:name pname :skill skill}]
     pilot))
  ([pilot]
   (->pilot (:name pilot) (:skill pilot))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Querying pilot data

(defn skill
  [pilot]
  (:skill pilot))

(defn full-name
  [pilot]
  (:name pilot))

(defn display
  [pilot]
  (str (full-name pilot) " (" (skill pilot) ")"))
