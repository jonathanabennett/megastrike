(ns megastrike.reports 
  (:require [clojure.java.io :as io]
            [com.brunobonacci.mulog :as mu]
            [megastrike.utils :as utils]))

(def reports (agent ""))

(def log-file (str utils/application-directory "megastrike.log"))

(io/delete-file log-file true)

(def logs
  (mu/start-publisher! {:type :multi
                        :publishers
                        [{:type :console :pretty? true} 
                         {:type :simple-file :filename log-file}]}))
