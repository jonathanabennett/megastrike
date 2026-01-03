(ns megastrike.server.server
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.battle-force :as bf]
   [org.httpkit.server :as hk-server]
   [megastrike.scenario :as scenario]))

(def empty-game
  {:units nil :forces nil :current-phase :lobby :map-width "1" :map-height "1" :turn-number 0})

(defonce game-state
  (atom {:forces []
         :units []
         :game-board []
         :current-phase :lobby
         :map-width 1
         :map-height 1
         :round-report ""
         :turn-order []
         :turn-flag false
         :turn-number 0}))

(def channels (atom {}))

(defonce server (atom nil))

(defmulti handler :action)

(defmethod handler :default
  [e]
  (mu/log ::unhandled-server-event
          :event e))

(defmethod handler ::shutdown
  [_]
  (mu/log ::server-shutdown))

(defmethod handler ::join-server
  [{:keys [player-id client]}]
  (when (not= player-id :observer)
    (let [player-force (bf/->battle-force player-id)
          forces (:forces @game-state)]
      (mu/log ::creating-player
              :player player-force
              :forces forces)
      (if (bf/select-force forces (:unit-group/keyword player-force))
        (swap! channels assoc client :observer)
        (let [new-forces (bf/update-battle-force forces (:unit-group/keyword player-force) player-force)]
          (swap! game-state assoc :forces new-forces)
          (mu/log ::force-created
                  :new-forces forces
                  :forces forces
                  :player player-force
                  :game-state @game-state)
          (swap! channels assoc client (:unit-group/keyword player-force)))))))

(defmethod handler ::load-scenario
  [{:keys [scenario player-id]}]
  (mu/log ::load-scenario
          :scenario scenario
          :player-id player-id)
  (reset! game-state (scenario/server-setup-scenario scenario)))

(defn on-open
  [ch]
  (swap! channels assoc ch :observer))

(defn stop-server
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn on-close
  [ch _]
  ((swap! channels dissoc ch)
   (when (empty? @channels)
     (mu/log ::shutdown-server
             :server @server)
     (stop-server))))

(defn on-receive
  [ws message]
  (mu/log ::message-received
          :message (read-string message))
  (let [msg (read-string message)]
    (handler (assoc msg :client ws)))
  (doseq [ch (keys @channels)]
    (mu/log ::messaging-client
            :game-state @game-state)
    (hk-server/send! ch (pr-str (assoc {} :event-type :server-update-received :game @game-state)))))

(defn app
  [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :headers {"content-type" "text/html"} :body "Connect WebSockets to this URL."}
    (hk-server/as-channel ring-req
                          {:on-open    on-open
                           :on-receive on-receive
                           :on-close   on-close})))

(defn launch-server []
  (reset! server (hk-server/run-server app {:port 8080})))
