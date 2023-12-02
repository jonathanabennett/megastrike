(ns megastrike.core
  (:require
   [cljfx.api :as fx]
   [clojure.core.cache :as cache]
   [clojure.string :as str]
   [megastrike.gui.events :as events]
   [megastrike.gui.views :as views]
   [megastrike.combat-unit :as cu]))

(def *state
  (atom
   (fx/create-context
    {:mul (cu/filter-membership cu/mul :type cu/ground-units)
     :force-name "AFFS"
     :force-color :gold
     :force-zone "N"
     :forces {}
     :units {}
     :active-unit nil
     :active-force nil
     :game-board []
     :current-phase -1
     :turn-number 0}
    cache/lru-cache-factory)))

(defn get-forces
  []
  (:forces @*state))

(defn add-force!
  [{name :name :as force}]
  (let [forces (get-forces)]
    (swap! *state #(assoc % :forces (assoc forces name force)))))

(defn del-force!
  [name]
  (let [forces (get-forces)]
    (swap! *state #(assoc % :forces (dissoc forces name)))))

(defn get-force
  [name]
  (get (get-forces) name))

(defn get-units
  []
  (:units @*state))

(defn add-unit!
  [unit]
  (let [units (get-units)
        full-name (:full-name unit)
        counter (count (remove #(not (str/includes? (first %) full-name)) units))
        id (if (= counter 0)
             (:full-name unit)
             (str full-name " #" (inc counter)))]
    (swap! *state #(assoc % :units (assoc units id unit)))))

(defn del-unit!
  [id]
  (let [units (get-units)]
    (swap! *state #(assoc % :units (dissoc units id)))))

(defn get-unit
  [id]
  (get (get-units) id))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
        {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
        {:context (fx/make-reset-effect *state)
         :dispatch fx/dispatch-effect})))

(def renderer
  (fx/create-renderer
    :middleware (comp
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_] {:fx/type views/root})))
    :opts {:fx.opt/map-event-handler event-handler
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(defn -main
  "I don't do a whole lot."
  []
  (fx/mount-renderer *state renderer))
