(ns megastrike.game
  (:require
   [clojure.string :as str]))

(def empty-game
  {:forces {}
   :units {}
   :active-unit nil
   :active-force nil
   :game-board []
   :current-phase -1
   :turn-number 0})

(defonce *game
  (atom empty-game))

(defn new-game
  []
  (reset! *game empty-game))

(defn get-forces
  []
  (:forces @*game))

(defn add-force!
  [{name :name :as force}]
  (let [forces (get-forces)]
    (swap! *game #(assoc % :forces (assoc forces name force)))))

(defn del-force!
  [name]
  (let [forces (get-forces)]
    (swap! *game #(assoc % :forces (dissoc forces name)))))

(defn get-force
  [name]
  (get (get-forces) name))

(defn get-units
  []
  (:units @*game))

(defn add-unit!
  [unit]
  (let [units (get-units)
        full-name (:full-name unit)
        counter (count (remove #(not (str/includes? (first %) full-name)) units))
        id (if (= counter 0)
             (:full-name unit)
             (str full-name " #" (inc counter)))]
    (swap! *game #(assoc % :units (assoc units id unit)))))

(defn del-unit!
  [id]
  (let [units (get-units)]
    (swap! *game #(assoc % :units (dissoc units id)))))

(defn get-unit
  [id]
  (get (get-units) id))
