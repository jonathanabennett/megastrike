(ns megastrike.movement 
  (:require [megastrike.board :as board] 
            [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]))

(defn find-path
  [unit destination board]
  (let [origin (board/find-hex unit board)
        mv-type (get unit :movement-mode :walk)]
    (board/astar origin destination board hex/hex-distance mv-type)))

(defn move-costs 
  [unit board]
  (let [origin (board/find-hex unit board)
        mv-type (get unit :movement-mode :walk)]
    (loop [sum [(board/step-cost origin (first (:path unit)) mv-type)]
                   path (:path unit)]
              (if (= (count path) 1) 
                sum 
                (recur (conj sum (board/step-cost (first path) (second path) mv-type)) 
                       (rest path))))))

(defn can-move?
  "Checks whether or not a unit can move from its location to a destination."
  [unit board]
  (cond
    (= (:movement-mode unit) :stand-still) (merge unit {:acted true :path []})
    (seq (:path unit)) 
    (let [sum (reduce + (move-costs unit board)) 
    ;; Add code here to default to walk OR the default movement mode 
           unit (if (not (:movement-mode unit))
                  (assoc unit :movement-mode :walk)
                  unit)
           move (cu/get-mv unit (:movement-mode unit))] 
       (if (<= sum move)
         (merge unit 
                (select-keys (last (:path unit)) [:p :q :r])
                {:acted true :path []})
         unit))))