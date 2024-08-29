(ns megastrike.movement 
  (:require [megastrike.board :as board] 
            [megastrike.combat-unit :as cu]
            [megastrike.hexagons.hex :as hex]))

(defn find-path
  [{:keys [movement-mode] :or {movement-mode :walk} :as unit} destination board]
  (board/astar (board/find-hex unit board) destination board hex/distance movement-mode))

(defn move-costs 
  [{:keys [movement-mode path] :or {movement-mode :walk} :as unit} board]
  (loop [sum [(board/step-cost (board/find-hex unit board) 
                               (first path)
                               movement-mode)] 
         path path] 
    (if (= (count path) 1) 
      sum 
      (recur (conj sum (board/step-cost (first path)
                                        (second path)
                                        movement-mode)) 
             (rest path)))))

(defn can-move?
  "Checks whether or not a unit can move from its location to a destination."
  [{:keys [path] :as unit} board]
  (cond
    (= (:movement-mode unit) :stand-still) (merge unit {:acted true :path []})
    (seq path) 
    (let [sum (reduce + (move-costs unit board)) 
    ;; Add code here to default to walk OR the default movement mode 
           unit (if (not (:movement-mode unit))
                  (assoc unit :movement-mode :walk)
                  unit)
           move (cu/get-mv unit (:movement-mode unit))] 
       (if (<= sum move)
         (merge unit 
                (select-keys (last path) [:p :q :r])
                {:acted true :path []})
         unit))))