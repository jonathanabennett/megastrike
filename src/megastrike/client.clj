(ns megastrike.client
  (:require
   [com.brunobonacci.mulog :as mu]
   [java-http-clj.websocket :as ws]
   [megastrike.server.server :as server])
  (:import
   [javafx.application Platform]))
;; megastrike.client

(defn create-connection
  ([address port on-message-fn]
   ;; 1. Create a StringBuilder to hold the incoming fragments
   (let [msg-buffer (StringBuilder.)]
     (ws/build-websocket
      (str "ws://" address ":" port)
      {:on-text (fn [_ data last?]
                  ;; 2. Append the current chunk to our buffer
                  (.append msg-buffer data)

                  ;; 3. Check if this is the final chunk
                  (when last?
                    ;; 4. Convert buffer to string and CLEAR the buffer for the next message
                    (let [full-message (.toString msg-buffer)]
                      (.setLength msg-buffer 0)

                      ;; 5. Parse and dispatch (Offload heavy parsing if possible, 
                      ;;    but for now we keep your structure)
                      (Platform/runLater
                       (fn []
                         (try
                           (let [message-map (read-string full-message)]
                             (on-message-fn message-map))
                           (catch Exception e
                             (mu/log ::parsing-error
                                     :message (.getMessage e)
                                      ;; Don't log full-message if it's huge
                                     :len (count full-message)))))))))
       :on-error (fn [_ throwable]
                   (mu/log ::error-received
                           :message (.getMessage throwable)))})))
  ([address on-message-fn]
   (create-connection address "8080" on-message-fn))
  ([on-message-fn]
   (create-connection "localhost" "8080" on-message-fn)))

;; (defn create-connection
;;   ([address port on-message-fn]
;;    (let [ws  (ws/build-websocket
;;               (str "ws://" address ":" port)
;;               {:on-text  (fn [ws string last?]
;;                            (Platform/runLater
;;                             (fn []
;;                               (try
;;                                 (let [message-map (read-string string)]
;;                                   (on-message-fn message-map))
;;                                 (catch Exception e
;;                                   (mu/log ::parsing-error
;;                                           :message (.getMessage e)
;;                                           :raw-string string))))))
;;                :on-error (fn [_ throwable]
;;                            (mu/log ::error-received
;;                                    :message (.getMessage throwable)))})]
;;      (mu/log ::connection-created
;;              :connection ws)
;;      ws))
;;   ([address on-message-fn]
;;    (create-connection address "8080" on-message-fn))
;;   ([on-message-fn]
;;    (create-connection "localhost" "8080" on-message-fn)))

(defn message-server
  [connection message]
  (when-let [ws-client connection]
    (ws/send ws-client (pr-str message))))

(defn deploy-unit-message
  [connection player active-unit]
  (message-server
   connection
   {:action ::server/deploy-unit
    :player player
    :unit active-unit}))

(defn load-scenario-message
  [connection scenario-file player-id]
  (message-server
   connection
   {:action ::server/load-scenario
    :scenario scenario-file
    :player player-id}))

(defn close-server
  [connection]
  (when-let [ws-client connection]
    (mu/log ::attempting-shutdown
            :ws ws-client)
    (ws/send ws-client (pr-str {:action :shutdown}))
    (ws/close ws-client 1000 "Client initiated shutdown")
    (reset! connection nil)))

