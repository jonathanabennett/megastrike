(ns megastrike.gui.views
  (:require [cljfx.api :as fx]
            [megastrike.gui.board.views :as board]
            [megastrike.gui.events :as events]
            [megastrike.gui.forces.views :as force]
            [megastrike.gui.lobby.views :as lobby]
            [megastrike.gui.subs :as sub]))

(defn command-palette [{:keys [fx/context]}]
  (let [phase (fx/sub-val context :current-phase)
        turn (fx/sub-val context :turn-number)
        turn-order (fx/sub-val context :turn-order)]
    {:fx/type :v-box
     :spacing 5
     :children [{:fx/type :label
                 :text (prn-str turn-order)}
                 {:fx/type :h-box
                 :children [{:fx/type :button
                             :text "Roll Initiative"
                             :on-action {:event-type ::events/roll-initiative :fx/sync true}}
                            {:fx/type :button
                             :text "Deploy Unit"
                             :on-action {:event-type ::events/deploy-unit :fx/sync true}}
                            {:fx/type :button
                             :text "Save Game"
                             :on-action {:event-type ::events/auto-save :fx/sync true}}]}]}))

(defn game-view [{:keys [fx/context]}]
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
     :title (sub/title-string context)
     :scene
     {:fx/type :scene
      :root
      (cond
        (= view :lobby) lobby/view
        (= view :game) {:fx/type game-view}
        :else lobby/view)}}))
