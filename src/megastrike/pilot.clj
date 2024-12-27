(ns megastrike.pilot
  (:require
   [clojure.string :as string]))

(defn ->pilot [name skill]
  (assert (string? name))
  (let [name (string/trim name)]
    (assert (not (empty? name)))
    (assert (<= 0 skill 8))
    {:name name :skill skill}))

(defn skill [pilot]
  (:skill pilot))
