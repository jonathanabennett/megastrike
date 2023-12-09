(ns megastrike.core
  (:require
   [cljfx.api :as fx]
   [clojure.core.cache :as cache]
   [megastrike.gui.events :as events]
   [megastrike.gui.views :as views]
   [megastrike.combat-unit :as cu])
  (:gen-class :main true))

(def *state
  (atom
   (fx/create-context
    {:mul (cu/filter-membership cu/mul :type cu/ground-units)
     :force-name "AFFS"
     :force-color :gold
     :force-zone "N"
     :pilot-name "Bob"
     :pilot-skill "4"
     :forces {} ;; Replace with a vector
     :units []
     :active-mul nil
     :active-force nil
     :active-unit nil
     :game-board []
     :map-width "16"
     :map-height "17"
     :current-phase -1
     :turn-number 0}
    cache/lru-cache-factory)))

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
  []
  (fx/mount-renderer *state renderer))
