(ns megastrike.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn load-resource
  [name]
  (-> name io/resource))

(defn strip-quotes
  [str]
  (string/replace str #"\"" ""))

(defn replace-spaces
  [str]
  (string/replace str " " "-"))

(defn correct-range-brackets
  [str]
  (string/replace (string/replace str "/-" "-0") "/" "-"))

(defn remove-parens
  [str]
  (string/replace str #"[\(\)]" ""))

(defn keyword-maker
  "Take a string with spaces, strips them out, and turns it into a keyword"
  [str]
  (let [ret (keyword (string/lower-case
                      (remove-parens
                       (correct-range-brackets
                        (replace-spaces (string/trim str))))))]
    (if (= ret (keyword ""))
      nil
      ret)))

(defn roll-die
  ([mods]
   ;; Rand-int is a half-open range, so we need to add 1 to get 1-6
   (+ (rand-int 6) 1 mods))
  ([]
   (roll-die 0)))

(defn roll2d
  ([mods]
   (+ (roll-die) (roll-die) mods))
  ([]
   (roll2d 0)))
