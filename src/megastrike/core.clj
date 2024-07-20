(ns megastrike.core
  (:gen-class
   :main true)
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [com.brunobonacci.mulog :as mu]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]
            [megastrike.gui.views :as views]
            [megastrike.hexagons.hex :as hex])
  (:import (javafx.application Platform)))

(mu/set-global-context! {:app-name "MegaStrike" :version "0.3.0"})

(def *state
  (atom
   (fx/create-context
    {:mul (cu/filter-units cu/mul :type cu/ground-units)
     :mul-search-term ""
     :display :lobby
     :title "Megastrike"
     :force-name "AFFS"
     :force-color :gold
     :force-zone "N"
     :pilot-name "Bob"
     :pilot-skill "4"
     :forces {}
     :units {}
     :ghosts []
     :internal {}
     :active-mul nil
     :active-force nil
     :active-unit nil
     :map-boards []
     :round-report ""
     :game-board []
     :layout (hex/create-layout)
     :map-width "1"
     :map-height "1"
     :current-phase :lobby
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
  "The main entry point for the game."
  []
  (mu/log ::launch-game)
  (Platform/setImplicitExit true)
  (fx/mount-renderer *state renderer))