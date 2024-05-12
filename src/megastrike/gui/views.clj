(ns megastrike.gui.views
  (:require [cljfx.api :as fx]
            [megastrike.gui.board.views :as board]
            [megastrike.gui.events :as events]
            [megastrike.gui.forces.views :as force]
            [megastrike.gui.lobby.views :as lobby]
            [megastrike.gui.subs :as subs]
            [clojure.string :as str]))

(defn deploy-buttons [{:keys [fx/context]}]
  (let [finished-deployment (empty? (subs/turn-order context))]
    {:fx/type :h-box
     :children [{:fx/type :button
                 :text "Roll Initiative"
                 :on-action {:event-type ::events/roll-initiative :fx/sync true}}
                {:fx/type :button ;; TODO Disable button when no units to deploy
                 :text "Deploy Unit"
                 :disable finished-deployment
                 :on-action {:event-type ::events/deploy-unit :fx/sync true}}
                {:fx/type :button ;; TODO Disable button when units left to deploy
                 :text "Next Phase"
                 :disable (not finished-deployment)
                 :on-action {:event-type ::events/next-phase :fx/sync true}}
                {:fx/type :separator
                 :orientation :vertical
                 :padding 15}
                {:fx/type :button
                 :text "Save Game"
                 :on-action {:event-type ::events/auto-save :fx/sync true}}]}))

(defn move-buttons [{:keys [fx/context]}]
  (let [active (fx/sub-val context :active-unit)
        units (subs/units context)
        unit (get units active)
        move-types (keys (:movement unit))
        buttons (for [[m] move-types] 
                       {:fx/type :button 
                        :text (str/capitalize (name m)) 
                        :on-action {:event-type ::events/set-movement-mode :mode m :fx/sync true}})]
    {:fx/type :h-box
     :children (conj [{:fx/type :button
                       :text "Stand Still"
                       :on-action {:event-type ::events/set-movement-mode :mode :standstill :fx/sync true}}]
                     buttons
                     [{:fx/type :button
                       :text "Confirm Move"
                       :on-action {:event-type ::events/move-unit :fx/sync true}}])}))

(defn command-palette [{:keys [fx/context]}]
  (let [phase (fx/sub-val context :current-phase)
        turn (fx/sub-val context :turn-number)
        turn-order (fx/sub-val context :turn-order)]
    {:fx/type :v-box
     :spacing 5
     :children [{:fx/type :label
                 :text (str phase " Phase | Turn " turn " | "(prn-str turn-order))}
                (cond 
                  (= phase :deployment){:fx/type deploy-buttons}
                  (= phase :movement) {:fx/type move-buttons}
                  :else {:fx/type :button
                         :text "Exit"
                         :on-action {:event-type ::events/quit-game :fx/sync true}})]}))

(def game-view
  {:fx/type :grid-pane
   :children [{:fx/type board/game-board
               :grid-pane/row 0
               :grid-pane/column 0}
              {:fx/type command-palette
               :grid-pane/row 1
               :grid-pane/column 0
               :grid-pane/column-span 2
               :grid-pane/hgrow :always
               :grid-pane/vgrow :always}
              {:fx/type force/stat-blocks
               :grid-pane/row 0
               :grid-pane/column 1
               :grid-pane/hgrow :always
               :grid-pane/vgrow :always}]})

(defn root [{:keys [fx/context]}]
  (let [view (fx/sub-val context :display)]
        
    {:fx/type :stage
     :showing true
     :title (subs/title-string context)
     :scene
     {:fx/type :scene
      :root
      (cond
        (= view :lobby) lobby/view
        (= view :game) game-view
        :else lobby/view)}}))
