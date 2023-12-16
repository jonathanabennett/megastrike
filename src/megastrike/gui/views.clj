(ns megastrike.gui.views
  (:require
   [cljfx.api :as fx]
   [megastrike.gui.lobby :as lobby]
   [megastrike.hexagons.hex :as hex]
   [megastrike.gui.common :as common]
   [megastrike.gui.events :as events]
   [megastrike.combat-unit :as cu]))

(defn draw-hex [{:keys [hex layout]}]
  (let [points (hex/hex-points hex layout)
        offset (hex/offset-from-hex hex)]
    {:fx/type :group
     :children [{:fx/type :polygon
                 :points points
                 :fill :green
                 :stroke :black}
                {:fx/type :label
                 :text (format "%02d%02d" (:x offset) (:y offset))
                 :layout-x (nth points 4)
                 :layout-y (nth points 5)
                 :font 16
                 :translate-x 10
                 :translate-y -20}]
     :on-mouse-clicked {:event-type ::events/hex-clicked
                        :fx/sync true
                        :hex hex}}))

(defn draw-unit [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points unit layout)
        force (first (filter #(= (:name %) (:force unit)) (fx/sub-val context :forces)))]
    {:fx/type :group
     :children [:fx/type common/draw-sprite
                :unit unit
                :force force
                :x (nth hex 8)
                :y (nth hex 9)]}))

(defn game-board [{:keys [fx/context]}]
  (let [gb (fx/sub-val context :game-board)
        layout (fx/sub-val context :layout)
        units (fx/sub-val context :units)]
    {:fx/type :scroll-pane
     :grid-pane/row 0
     :grid-pane/column 0
     :grid-pane/hgrow :always
     :grid-pane/vgrow :always
     :content {:fx/type :group
               :children (;; concat
                          (for [h gb]
                            {:fx/type draw-hex
                             :hex h
                             :layout layout})
                          ;; (for [u units]
                          ;;   (when (:q u)
                          ;;     {:fx/type draw-unit
                          ;;      :unit u
                          ;;      :layout layout}))
                          )}}))

(defn command-palette [{:keys [fx/context]}]
  (let [phase (fx/sub-val context :current-phase)
        turn (fx/sub-val context :turn-number)]
    {:fx/type :label
     :grid-pane/row 1
     :grid-pane/column 0
     :grid-pane/hgrow :always
     :grid-pane/vgrow :always
     :text "Command Buttons"}))

(defn unit-stat-block [{:keys [unit]}]
  {:fx/type :v-box
   :spacing 5
   :children[{:fx/type :h-box
              :spacing 5
              :children [{:fx/type :label
                          :text (str "Unit: " (:full-name unit))}
                         {:fx/type :label
                          :text (str "Type: " (:type unit))}
                         {:fx/type :label
                          :text (str "Role: " (:role unit))}
                         {:fx/type :label
                          :text (str "Size: " (:size unit))}
                         {:fx/type :label
                          :text (str "TMM: " (:tmm unit))}
                         {:fx/type :label
                          :text (str "Mv: " (cu/print-movement unit))}
                         {:fx/type :label
                          :text (str "Pilot: " (:pilot/name unit) " (" (:pilot/skill unit) ")")}]}
             ]})

(def game-view
  {:fx/type :grid-view
   :children [{:fx/type :label
               :grid-pane/row 0
               :grid-pane/column 0
               :text "Game View"}
              ;;{:fx/type game-board}
              ;;(:fx/type command-palette)
              ]})

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
