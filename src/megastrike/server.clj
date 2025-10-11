(ns megastrike.server
  (:require
   [com.brunobonacci.mulog :as mu]
   [org.httpkit.server :as hk-server]
   [megastrike.core :as core]
   [megastrike.hexagons.hex :as hex]))

(defonce game-state (atom {}))

(def channels (atom #{}))

(defn on-open    [ch]   (swap! channels conj ch))
(defn on-close   [ch _] (swap! channels disj ch))

(defn on-receive [_ message]
  (let [msg (read-string message)]
    (when (= (:action msg) :increment)
      (swap! game-state update :counter inc)))
  (mu/log ::message-received
          :new-state (pr-str @game-state))
  (doseq [ch @channels]
    (hk-server/send! ch (pr-str @game-state))))

(defn app [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :headers {"content-type" "text/html"} :body "Connect WebSockets to this URL."}
    (hk-server/as-channel ring-req
                          {:on-open    on-open
                           :on-receive on-receive
                           :on-close   on-close})))

(defn launch-server [] (hk-server/run-server app {:port 8080}))

(defn unoccupied
  [hex units]
  (some #(hex/same-hex hex %) units))

(defn deploy-unit
  "Check if a unit can deploy in a given hex. If they can, set their location
  to that hex. If they can't, return an error explaining why."
  [{:keys [unit]} {:keys [units turn-order] :as game-state}]
  (if (and (= (:battle-force unit) (first turn-order))
           (unoccupied unit units))
    (let [upd (assoc unit :acted true)]
      (swap! game-state assoc
             :units (assoc units (:id upd) upd)
             :turn-order (rest turn-order)
             :active-unit nil)
      game-state)
    game-state))

(defn update-game-state
  [{:keys [state]} game-state]
  (swap! game-state merge game-state state))

(defn game-event-handler
  [{:keys [event-id] :as data}]
  (condp event-id
         :deploy-unit (deploy-unit data core/*state)
         (update-game-state data core/*state)))
