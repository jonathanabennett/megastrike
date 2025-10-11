(ns megastrike.core
  "This launches the Megastrike Game. State management, development flags, etc
  are all handled from here."
  (:gen-class
   :main true)
  (:require
   [cljfx.api :as fx]
   [clojure.core.cache :as cache]
   [com.brunobonacci.mulog :as mu]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.events :as events]
   [megastrike.schemas :as schemas]
   [megastrike.gui.views :as views]
   [megastrike.hexagons.hex :as hex])
  (:import
   (javafx.application Platform)))

(mu/set-global-context! {:app-name "MegaStrike" :version "0.5.0"})

(def in-development? true)

(def *state
  (atom
   (fx/create-context
    {:lobby {:mul (cu/filter-units cu/mul :mul/ground-units)
             :mul-search-term ""
             :force-zone "N"
             :player :player
             :pilot-name "Bob"
             :pilot-skill "4"
             :active-mul nil
             :map-boards []
             :force-name "AFFS"}
     :game {:forces []
            :units []
            :game-board []
            :active-unit nil
            :current-phase :lobby
            :map-width "1"
            :map-height "1"
            :round-report ""
            :turn-order []
            :turn-flag false
            :turn-number 0}
     :gui {:lobby-view true
           :game-view false
           :title "Megastrike"
           :dialogs {:attack-dialog {:showing false
                                     :items []
                                     :unit nil}
                     :round-dialog {:showing false
                                    :advance-phase? false}}
           :layout (hex/create-layout)}}
    cache/lru-cache-factory)))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect})))

(def type->lifecycle #(or (fx/keyword->lifecycle %)
                          (fx/fn->lifecycle-with-context %)))

(def renderer
  (fx/create-renderer
   :middleware (comp
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type views/root})))
   :error-handler (bound-fn [^Throwable ex]
                    (.printStackTrace ^Throwable ex *err*))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle (if in-development?
                                    (@(requiring-resolve 'cljfx.dev/wrap-type->lifecycle) {:type->lifecycle type->lifecycle})
                                    type->lifecycle)}))

(defn dev-launch
  []
  (mu/log ::launch-game
          :development true)
  (fx/mount-renderer *state renderer))

(defn regular-launch
  []
  (mu/log ::launch-game)
  (Platform/setImplicitExit true)
  (fx/mount-renderer *state renderer))

(defn -main
  "The main entry point for the game."
  []
  (if in-development?
    (dev-launch)
    (regular-launch)))
