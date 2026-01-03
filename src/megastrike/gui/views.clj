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
  (let [props (subs/attack-dialog context)
        unit (:units props)
        attacks (:items props)
        phase (subs/phase context)
        active (subs/active-unit context)
        mv-type (if active (movement/selected-or-default active) :walk)]
    {:fx/type :dialog
     :showing (:showing props)
     :on-close-request (fn [^DialogEvent event]
                         (when (nil? (.getResult ^Dialog (.getSource event)))
                           (.consume event)))
     :header-text (str (:unit/full-name active) " attacking " (:unit/full-name unit))
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
        round-report (subs/round-report context)
        props (subs/round-dialog context)]
    {:fx/type :dialog
     :showing (:showing props)
     :header-text (str "Turn " round " / " phase " phase")
     :on-hidden {:event-type ::events/close-round-dialog :phase-advance? false}
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:ok]
                   :content {:fx/type :scroll-pane
                             :content {:fx/type :text
                                       :text (or round-report "")}}}}))

(defn game-board
  [{:keys [fx/context]}]
  (let [gb (subs/tiles context)
        layout (subs/layout context)
        units (subs/units context)
        unit-locations (filter movement/deployed? units)
        destinations (filter #(pos? (count (:unit/path %))) (subs/units context))]
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

(defn game-view [_]
  {:fx/type :grid-pane
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
               :grid-pane/vgrow :always}]})
;; {:fx/type :stage
;;  :showing (subs/game-view context)
;;  :title (subs/title-string context)
;;  :scene {:fx/type :scene
;;            ;; :accelerators {[:minus] {:event-type ::events/change-size :direction :minus :fx/sync true}
;;            ;;                [:shift :equals] {:event-type ::events/change-size :direction :plus}}
;;          :root {:fx/type :grid-pane
;;                 :children [{:fx/type game-board
;;                             :grid-pane/row 0
;;                             :grid-pane/column 0}
;;                            {:fx/type elements/command-palette
;;                             :grid-pane/row 1
;;                             :grid-pane/column 0
;;                             :grid-pane/column-span 2
;;                             :grid-pane/hgrow :always
;;                             :grid-pane/vgrow :always}
;;                            {:fx/type elements/stat-blocks
;;                             :grid-pane/row 0
;;                             :grid-pane/column 1
;;                             :grid-pane/hgrow :always
;;                             :grid-pane/vgrow :always}]}}}

(defn lobby-view [_]
  {:fx/type :grid-pane
   :children [lobby/force-pane
              lobby/unit-pane
              lobby/map-pane]})

(defn main-menu-view [_]
  {:fx/type :v-box
   :alignment :center
   :spacing 20
   :children [{:fx/type :label
               :style {:-fx-font-size 30}
               :text "Megastrike"}
              {:fx/type elements/text-input
               :alignment :center
               :label "Force Name: "
               :ks [:client :player-id]}
              {:fx/type :button
               :text "Host New Game"
               :on-action {:event-type ::events/host-game}}
              {:fx/type elements/text-input
               :alignment :center
               :label "Server IP: "
               :ks [:client :server-ip]}
              {:fx/type :button
               :text "Join Network Game"
               :on-action {:event-type ::events/join-network-game}}
              {:fx/type :button
               :text "Load Scenario"
               :on-action {:event-type ::events/load-scenario}}]})

(defn main-window-view [{:keys [fx/context] :as state}]
  (let [app-phase (fx/sub-val context :app-phase)]
    {:fx/type :stage
     :showing true
     :title "Megastrike"
     :width 1024
     :height 768
     ;; THE ROUTER: Swaps the scene content based on state
     :scene {:fx/type :scene
             :root (case app-phase
                     :main-menu {:fx/type main-menu-view}
                     :lobby     {:fx/type lobby-view}
                     :game      {:fx/type game-view}
                                                                   ;; Default fall-through
                     {:fx/type :v-box
                      :children [{:fx/type :label :text "Unknown State"}]})}}))

(defn root [{:keys [gui app-phase] :as state}]
  {:fx/type fx/ext-many
   :desc (concat
           ;; 1. The Main Application Window
          [{:fx/type main-window-view
            :app-phase app-phase}]

           ;; 2. Conditional Dialogs
           ;; Only render the component if the state says it is showing.
           ;; This prevents "hidden" windows from consuming resources.
          (when (get-in gui [:dialogs :attack-dialog :showing])
            [{:fx/type attack-dialog}])

          (when (get-in gui [:dialogs :round-dialog :showing])
            [{:fx/type round-dialog}]))})

