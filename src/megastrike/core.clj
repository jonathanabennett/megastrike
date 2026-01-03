(ns megastrike.core
  "This launches the Megastrike Game. State management, development flags, etc
  are all handled from here."
  (:gen-class
   :main true)
  (:require
   [cljfx.api :as fx]
   [clojure.core.cache :as cache]
   [com.brunobonacci.mulog :as mu]
   [megastrike.client :as client]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.events :as events]
   [megastrike.gui.views :as views]
   [megastrike.hexagons.hex :as hex]
   [megastrike.server.server :as server]))

(def *state
  (atom
   (fx/create-context
    {:app-phase :main-menu
     :client {:socket nil
              :player-id ""
              :server-ip "127.0.0.1"
              :connection-status :disconnected}
     :lobby {:mul (cu/filter-units cu/mul :mul/ground-units)
             :mul-search-term ""
             :pilot-name "Bob"
             :pilot-skill "4"
             :active-mul nil
             :map-boards []}
     :game {:forces []
            :units []
            :game-board []
            :current-phase :lobby
            :map-width 1
            :map-height 1
            :round-report ""
            :turn-order []
            :turn-flag false
            :turn-number 0}
     :gui {:active-unit nil
           :title "Megastrike"
           :dialogs
           {:force-creation-dialog
            {:showing false
             :player :player
             :name "AFFS"
             :camo nil
             :zone :deployment/any}
            :attack-dialog
            {:showing false
             :items []
             :unit nil}
            :round-dialog
            {:showing false
             :advance-phase? false}}
           :layout (hex/create-layout)}}
    cache/lru-cache-factory)))

(mu/set-global-context! {:app-name "MegaStrike" :version "0.5.0"})

(def in-development? true)

(defn create-connection-effect
  [{:keys [action player-id]} dispatch!]
  (future
    (when (= action :host)
      (server/launch-server)
      (Thread/sleep 500)))

  (let [conn (client/create-connection "localhost" dispatch!)]

    (client/message-server conn {:action :join-server :player-id player-id})
    (dispatch! {:event-type ::events/connection-established
                :connection conn
                :player-id player-id})))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect
        :connect create-connection-effect})))

(def type->lifecycle #(or (fx/keyword->lifecycle %)
                          (fx/fn->lifecycle-with-context %)))

(defn renderer
  [in-development?]
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

(defn -main
  "The main entry point for the game."
  []
  (mu/log ::launch-game
          :development in-development?)
  ;; (future (server/launch-server))
  ;; (client/launch-client "localhost" "8080" (fn [message-map] (events/dispatch-event message-map)))
  (fx/mount-renderer *state (renderer in-development?)))
