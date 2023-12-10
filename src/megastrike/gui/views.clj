(ns megastrike.gui.views
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.table-view :as tables]
   [megastrike.gui.events :as events]
   [megastrike.gui.common :as common]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.subs :as sub]
   [megastrike.gui.lobby :as lobby]))

(def game-view
  {:fx/type :label
   :text "Game view"})

(defn root [{:keys [fx/context]}]
  (let [view (fx/sub-val context :display)]
    {:fx/type :stage
     :showing true
     :scene
     {:fx/type :scene
      :root
      (cond
        (= view :lobby) lobby/view
        (= view :game) game-view
        :else lobby/view)}}))
