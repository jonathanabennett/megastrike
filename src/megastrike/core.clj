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
   [megastrike.gui.views :as views]
   [megastrike.hexagons.hex :as hex]
   [megastrike.schemas :as schemas])
  (:import
   (javafx.application Platform)))

(mu/set-global-context! {:app-name "MegaStrike" :version "0.5.0"})

(def in-development? false)

(def *state
  (atom
   (fx/create-context
    {:mul (cu/filter-units cu/mul :type schemas/ground-units)
     :mul-search-term ""
     :display :lobby
     :title "Megastrike"
     :force-name "AFFS"
     :force-zone "N"
     :player :player
     :pilot-name "Bob"
     :pilot-skill "4"
     :forces {}
     :units {}
     :internal {}
     :round-dialog {:showing false
                    :advance-phase? false}
     :active-mul nil
     :active-force nil
     :active-unit nil
     :map-boards []
     :round-report ""
     :game-board (board/create-board 16 17)
     :layout (hex/create-layout)
     :map-width "1"
     :map-height "1"
     :lobby true
     :game false
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
