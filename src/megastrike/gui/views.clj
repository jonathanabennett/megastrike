(ns megastrike.gui.views
  (:require [megastrike.gui.board.views :as board]
            [megastrike.gui.events :as events]
            [megastrike.gui.forces.views :as force]
            [megastrike.gui.lobby.views :as lobby]
            [megastrike.gui.subs :as subs]
            [clojure.string :as str]))

(defn deploy-buttons [finished-deployment]
     [{:fx/type :button
       :text "Deploy Unit"
       :disable finished-deployment
       :on-action {:event-type ::events/deploy-unit :fx/sync true}}
      {:fx/type :button
       :text "Undeploy Unit"
       :on-action {:event-type ::events/undeploy-unit :fx/sync true}}])

(defn move-buttons [unit]
  (let [ movement (:movement unit)
        buttons (if (contains? movement :jump)
                  [{:fx/type :button
                    :text "Walk"
                    :on-action {:event-type ::events/set-movement-mode :mode :walk :unit unit :fx/sync true}}
                   {:fx/type :button
                    :text "Jump"
                    :on-action {:event-type ::events/set-movement-mode :mode :jump :unit unit :fx/sync true}}]
                  [{:fx/type :button
                    :text "Walk"
                    :on-action {:event-type ::events/set-movement-mode :mode :walk :unit unit :fx/sync true}
                    }])] 
     ((comp vec flatten vector) 
      [{:fx/type :button 
        :text "Stand Still" 
        :on-action {:event-type ::events/set-movement-mode :mode :stand-still :unit unit :fx/sync true}}] 
      buttons 
      [{:fx/type :button 
        :text "Confirm Move" 
        :on-action {:event-type ::events/confirm-move :unit unit :fx/sync true}}])))

(defn attack-buttons [] 
  [{:fx/type :button 
    :text "Overheat +1" 
    :on-action {:event-type ::events/overheat :value 1}} 
   {:fx/type :button 
    :text "Overheat -1"
    :on-action {:event-type ::events/overheat :value -1}} 
   {:fx/type :button 
    :text "Resolve Attacks" 
    :on-action {:event-type ::events/make-attacks}}])

(defn command-palette [{:keys [fx/context]}]
  (let [phase (subs/phase context)
        turn (subs/turn-number context)
        turn-order (subs/turn-order context) 
        unit (subs/active-unit context)
        common-buttons [{:fx/type :button
                         :text "Next Phase"
                         :disable (seq turn-order)
                         :on-action {:event-type ::events/next-phase :fx/sync true}}
                        {:fx/type :separator
                         :orientation :vertical 
                         :padding 15}
                        {:fx/type :button 
                         :text "Save Game"
                         :on-action {:event-type ::events/auto-save :fx/sync true}}
                         {:fx/type :button
                          :text "Zoom In"
                          :on-action {:event-type ::events/change-size :direction :plus :fx/sync true}}
                         {:fx/type :button
                          :text "Zoom Out"
                          :on-action {:event-type ::events/change-size :direction :minus :fx/sync true}}
                        {:fx/type :button :text "Exit"
                         :on-action {:event-type ::events/quit-game}}]
        phase-buttons (cond 
                        (= phase :deployment) (deploy-buttons (empty? turn-order))
                        (= phase :movement) (move-buttons unit)
                        (= phase :combat) (attack-buttons)
                        :else []) 
        buttons ((comp vec flatten vector) phase-buttons common-buttons)]
    {:fx/type :v-box
     :spacing 5
     :children [{:fx/type :label
                 :text (str (str/capitalize (name phase)) " Phase | Turn " (str turn) " | " (prn-str turn-order))} 
                {:fx/type :h-box 
                 :children buttons}
                ]}))

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
  (let [view (subs/get-view context)]
        
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
