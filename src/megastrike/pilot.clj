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

(defprotocol Crew
  (full-name [this])
  (skill [this])
  (display [this] "Crew are displayed as a string like so: name (skill). Like 'Bob Kim (4)'")
  (set-full-name [this new-name])
  (set-skill [this new-skill] "Sets the skill of a pilot IF it is a valid skill (between 0 and 8)")
  (kills [this])
  (set-kills [this new-kills])
  (add-kill [this]))

(defn valid-pilot-skill?
  [n]
  (<= 0 n 8))

(defn clean-pilot-skill
  [n]
  (cond
    (< n 0) 0
    (> n 8) 8
    :else n))

(defrecord Pilot [full-name skill kills]
  Crew
  (full-name [_] full-name)
  (skill [_] skill)
  (display [_] (str full-name "( " skill ")"))
  (set-full-name [this new-name] (assoc this :full-name new-name))
  (set-skill [this new-skill] (when (valid-pilot-skill? new-skill) (assoc this :skill new-skill)))
  (kills [_] kills)
  (set-kills [this new-kills] (assoc this :kills new-kills))
  (add-kill [this] (assoc this :kills (inc (kills this)))))

(defn create-pilot
  ([pname skill kills]
   (let [pname (string/trim pname)
         skill (if (= (type skill) java.lang.String)
                 (Integer/parseInt skill)
                 skill)]
     (->Pilot pname (clean-pilot-skill skill) kills)))
  ([pname skill]
   (create-pilot pname skill 0)))

