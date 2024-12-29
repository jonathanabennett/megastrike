(ns megastrike.pilot
  "Handles pilots and crews for units.
  
  `->pilot` creates a pilot from a name and skill
  `skill` gets the pilot's skill value as an int
  `fullname` gets the pilot's name
  `display` prints the pilot's stats as name(skill), so 'Bob Kim(4)'"
  (:require
   [clojure.string :as string]
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema and constructor
(defn valid-pilot-skill?
  [n]
  (<= 0 n 8))

(def PilotSchema
  [:map [:name string?] [:skill valid-pilot-skill?]])

(defn ->pilot
  ([name skill]
   (let [name (string/trim name)
         skill (if (= (type skill) java.lang.String)
                 (Integer/parseInt skill)
                 skill)
         pilot {:name name :skill skill}]
     pilot))
  ([pilot]
   (->pilot (:name pilot) (:skill pilot))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Querying pilot data

(defn skill
  [pilot]
  (:skill pilot))

(defn skill-mod
  [pilot]
  [{:desc "Pilot skill" :value (skill pilot)}])

(defn fullname
  [pilot]
  (:name pilot))

(defn display
  [pilot]
  (str (pilot :name) " (" (pilot :skill) ")"))
