(ns megastrike.gui.views
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [megastrike.attacks :as attacks]
            [megastrike.gui.board.views :as board]
            [megastrike.gui.events :as events]
            [megastrike.gui.forces.views :as force]
            [megastrike.gui.lobby.views :as lobby]
            [megastrike.gui.subs :as subs]))

(defn attack-report-button
  [{:keys [fx/context state-id on-confirmed button dialog-pane]}]
  {:fx/type fx/ext-let-refs
   :refs {::dialog {:fx/type :dialog
                    :showing (fx/sub-val context get-in [:internal state-id :showing] false)
                    :on-hidden {:event-type ::events/hide-popup
                                :state-id state-id
                                :on-confirmed on-confirmed}
                    :dialog-pane (merge {:fx/type :dialog-pane
                                         :button-types [:ok]}
                                        dialog-pane)}}
   :desc (merge {:fx/type :button
                 :on-action {:event-type ::events/show-popup
                             :state-id state-id}}
                button)})

(defn next-phase-button
  [{:keys [fx/context state-id on-confirmed button dialog-pane]}]
  {:fx/type fx/ext-let-refs
   :refs {::dialog {:fx/type :dialog
                    :showing (fx/sub-val context get-in [:internal state-id :showing] false)
                    :on-hidden {:event-type ::events/hide-popup
                                :state-id state-id
                                :on-confirmed on-confirmed}
                    :dialog-pane (merge {:fx/type :dialog-pane
                                         :button-types [:ok]}
                                        dialog-pane)}}
   :desc (merge {:fx/type :button
                 :on-action {:event-type ::events/next-phase
                             :state-id state-id}}
                button)})

(defn deploy-buttons [finished-deployment]
  [{:fx/type :button
    :text "Deploy Unit"
    :disable finished-deployment
    :on-action {:event-type ::events/deploy-unit :fx/sync true}}
   {:fx/type :button
    :text "Turn"
    :on-action {:event-type ::events/turn-button-clicked :fx/sync true}}
   {:fx/type :button
    :text "Undeploy Unit"
    :on-action {:event-type ::events/undeploy-unit :fx/sync true}}])

(defn move-buttons [{:keys [movement] :as unit}]
  (let [buttons (if (contains? movement :jump)
                  [{:fx/type :button
                    :text "Walk"
                    :on-action {:event-type ::events/set-movement-mode :mode :walk :unit unit :fx/sync true}}
                   {:fx/type :button
                    :text "Jump"
                    :on-action {:event-type ::events/set-movement-mode :mode :jump :unit unit :fx/sync true}}]
                  [{:fx/type :button
                    :text "Walk"
                    :on-action {:event-type ::events/set-movement-mode :mode :walk :unit unit :fx/sync true}}])]
    ((comp vec flatten vector)
     [{:fx/type :button
       :text "Stand Still"
       :on-action {:event-type ::events/set-movement-mode :mode :stand-still :unit unit :fx/sync true}}
      {:fx/type :button
       :text "Turn"
       :on-action {:event-type ::events/turn-button-clicked :fx/sync true}}]
     buttons
     [{:fx/type :button
       :text "Cancel Move"
       :on-action {:event-type ::events/cancel-move :unit unit :fx/sync true}}
      {:fx/type :button
       :text "Confirm Move"
       :on-action {:event-type ::events/confirm-move :unit unit :fx/sync true}}])))

(defn attack-buttons [units current-force board]
  [{:fx/type :button
    :text "Overheat +1"
    :on-action {:event-type ::events/overheat :value 1}}
   {:fx/type :button
    :text "Overheat -1"
    :on-action {:event-type ::events/overheat :value -1}}
   {:fx/type attack-report-button
    :state-id ::attack-info
    :button {:text "Review Attacks"}
    :dialog-pane {:content-text (attacks/generate-attack-info units current-force board)}
    :on-confirmed {:event-type ::events/no-op}}
   {:fx/type :button
    :text "Clear Target"
    :on-action {:event-type ::events/clear-target}}
   {:fx/type :button
    :text "Finish Attacks"
    :on-action {:event-type ::events/finish-attacks}}])

(defn command-palette [{:keys [fx/context]}]
  (let [phase (subs/phase context)
        turn (subs/turn-number context)
        turn-order (subs/turn-order context)
        unit (subs/active-unit context)
        units (subs/units context)
        current-force (subs/current-forces context)
        board (subs/board context)
        common-buttons [{:fx/type next-phase-button
                         :state-id ::next-phase-button
                         :button {:text "Next Phase"}
                         :dialog-pane {:content-text (fx/sub-val context :round-report)}
                         :on-confirmed {:event-type ::events/no-op}}
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
                        (= phase :combat) (attack-buttons units current-force board)
                        :else [])
        buttons ((comp vec flatten vector) phase-buttons common-buttons)]
    {:fx/type :v-box
     :spacing 5
     :children [{:fx/type :label
                 :text (str (str/capitalize (name phase)) " Phase | Turn " (str turn) " | " (prn-str turn-order))}
                {:fx/type :h-box
                 :children buttons}]}))

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
      :accelerators {[:minus] {:event-type ::events/change-size :direction :minus :fx/sync true}
                     [:shift :equals] {:event-type ::events/change-size :direction :plus}}
      :root
      (cond
        (= view :lobby) lobby/view
        (= view :game) game-view
        :else lobby/view)}}))
