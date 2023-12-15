(ns megastrike.initiative
  (:require [megastrike.utils :as utils]))

;; Let's handle initiative by storing the initiative roll in
;; the force map and adding an `acted?` key to units. We can then
;; just select whose next.

(defn roll-initiative
  [forces]
  (merge (for [f forces] {:name (:name f) :roll (utils/roll2d)})))

(defn frob [forces]
  (let [rolls (zipmap forces (repeatedly utils/roll2d))]
    (if-not (apply distinct? (vals rolls))
      (recur forces)
      rolls)))
