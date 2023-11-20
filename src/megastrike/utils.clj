(ns megastrike.utils
  (:require [clojure.string :as string]))

(defn strip-quotes
  [str]
  (string/replace str #"\"" ""))

(defn keyword-maker
  "Take a string with spaces, strips them out, and turns it into a keyword"
  [str]
  (keyword (string/lower-case (string/replace str " " "-"))))
