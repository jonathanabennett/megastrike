(ns megastrike.gui.views
  (:require
   [cljfx.api :as fx]
   [megastrike.gui.elements :as elements]
   [megastrike.gui.events :as events]
   [megastrike.gui.lobby.views :as lobby]
   [megastrike.gui.subs :as subs]
   [megastrike.movement :as movement])
  (:import
   [javafx.scene.control Dialog DialogEvent]))

(defn attack-dialog
  [{:keys [fx/context]}]
  (let [unit (fx/sub-val context get-in [:internal :attack-dialog :unit])
        attacks (fx/sub-val context get-in [:internal :attack-dialog :items])
        phase (subs/phase context)
        active (subs/active-unit context)
        mv-type (if active (movement/selected-or-default active) :walk)]
    {:fx/type :dialog
     :showing (fx/sub-val context get-in [:internal :attack-dialog :showing] false)
     :on-close-request (fn [^DialogEvent event]
                         (when (nil? (.getResult ^Dialog (.getSource event)))
                           (.consume event)))
     :header-text (str (:full-name active) " attacking " (:full-name unit))
     :on-hidden {:event-type ::events/close-attack-selection
                 :unit unit
                 :on-close {:event-type ::events/make-attack :unit unit}}
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:cancel]
                   :content {:fx/type :v-box
                             :spacing 5
                             :children (elements/attack-buttons attacks unit phase mv-type)}}}))

(defn round-dialog
  [{:keys [fx/context]}]
  (let [round (subs/turn-number context)
        phase (name (subs/phase context))
        round-report (subs/round-report context)]
    ; (mu/log ::round-report
    ;         :round round
    ;         :phase phase
    ;         :report round-report)
    {:fx/type :dialog
     :showing (fx/sub-val context get-in [:round-dialog :showing] false)
     :header-text (str "Turn " round " / " phase " phase")
     :on-hidden {:event-type ::events/close-round-dialog :phase-advance? false}
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:ok]
                   :content {:fx/type :scroll-pane
                             :content {:fx/type :text
                                       :text round-report}}}}))

(defn game-board
  [{:keys [fx/context]}]
  (let [gb (subs/tiles context)
        layout (subs/layout context)
        units (vals (subs/units context))
        unit-locations (filter movement/deployed? units)
        destinations (filter #(pos? (count (:unit/path %))) (vals (subs/units context)))]
    {:fx/type :scroll-pane
     :content {:fx/type :group
               :children (concat
                          (for [h gb]
                            {:fx/type elements/draw-hex
                             :hex h
                             :layout layout})
                          (for [t unit-locations]
                            {:fx/type elements/draw-unit
                             :unit t
                             :layout layout})
                          (when (seq destinations)
                            (for [t destinations]
                              {:fx/type elements/draw-movement-path
                               :unit t
                               :layout layout})))}}))

(defn game-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :showing (fx/sub-val context :game)
   :title (subs/title-string context)
   :scene {:fx/type :scene
           :accelerators {[:minus] {:event-type ::events/change-size :direction :minus :fx/sync true}
                          [:shift :equals] {:event-type ::events/change-size :direction :plus}}
           :root {:fx/type :grid-pane
                  :children [{:fx/type game-board
                              :grid-pane/row 0
                              :grid-pane/column 0}
                             {:fx/type elements/command-palette
                              :grid-pane/row 1
                              :grid-pane/column 0
                              :grid-pane/column-span 2
                              :grid-pane/hgrow :always
                              :grid-pane/vgrow :always}
                             {:fx/type elements/stat-blocks
                              :grid-pane/row 0
                              :grid-pane/column 1
                              :grid-pane/hgrow :always
                              :grid-pane/vgrow :always}]}}})

(defn lobby-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :showing (fx/sub-val context :lobby)
   :title (subs/title-string context)
   :width 800
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :grid-pane
                  :children [lobby/force-pane
                             lobby/unit-pane
                             lobby/map-pane]}}})

(defn root [_]
  {:fx/type fx/ext-many
   :desc [{:fx/type game-view}
          {:fx/type lobby-view}
          {:fx/type attack-dialog}
          {:fx/type round-dialog}]})
