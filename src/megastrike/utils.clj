(ns megastrike.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def application-directory (subs (.getAbsolutePath (io/file ".")) 0 (dec (.length (.getAbsolutePath (io/file "."))))))

(def data-directory (io/file application-directory "data/"))

(defn load-resource
  "Helper function to use `io/resource` to get files."
  [location name]
  (if (= location :resources)
    (io/resource name)
    (io/file data-directory name)))

(defn strip-quotes
  "Strips out stray escaped quotes."
  [str]
  (string/replace str #"\"" ""))

(defn replace-spaces
  "Replace spaces with `-'."
  [str]
  (string/replace str " " "-"))

(defn correct-range-brackets
  "Changes range brackets to be in a format which can be read by combat_unit.clj"
  [str]
  (string/replace (string/replace str "/-" "-0") "/" "-"))

(defn remove-parens
  "Removes all parents from a string."
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
  "Rolls a random die. Rand-int is a half-open range, so we need to add 1 to get a 6 sided die."
  ([mods]
   (+ (rand-int 6) 1 mods))
  ([]
   (roll-die 0)))

(defn roll2d
  "Rolls 2d6. With no argument, it rolls with no mods. With an argument, it applies that as a mod to the roll."
  ([mods]
   (+ (roll-die) (roll-die) mods))
  ([]
   (roll2d 0)))
