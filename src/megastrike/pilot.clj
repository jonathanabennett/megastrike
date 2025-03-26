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
  ([pname skill kills]
   (let [pname (string/trim pname)
         skill (if (= (type skill) java.lang.String)
                 (Integer/parseInt skill)
                 skill)
         pilot {:pilot/name pname :pilot/skill skill :pilot/kills kills}]
     pilot))
  ([pilot]
   (->pilot (:name pilot) (:skill pilot) (get pilot :kills 0))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Querying pilot data

(defn display
  "Formats the pilot information in the following format: 'Name(skill)'
  Examples:
  Bob Kim(4)
  Shooty McShootyface (2)"
  [pilot]
  (str (:pilot/name pilot) " (" (:pilot/skill pilot) ")"))
