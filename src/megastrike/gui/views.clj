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
     :on-mouse-clicked {:event-type ::events/hex-clicked :hex hex}
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
                        }))

(defn draw-unit [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points unit layout)
        force (first (filter #(= (:name %) (:force unit)) (fx/sub-val context :forces)))]
    {:fx/type :group
     :on-mouse-clicked {:event-type ::events/unit-clicked :unit unit}
     :children [:fx/type common/draw-sprite
                :unit unit
                :force force
                :x (nth hex 8)
                :y (nth hex 9)]}))

(defn game-board [{:keys [fx/context]}]
  (let [gb (fx/sub-val context :game-board)
        layout (fx/sub-val context :layout)
        units (fx/sub-val context :units)
        tokens (loop [i 0
                      tokens []]
                 (if (= (count units) i)
                   tokens
                   (recur
                    (inc i)
                    (if (:q (units i))
                      (conj tokens {:fx/type draw-unit
                                    :unit (units i)
                                    :layout layout})
                      tokens))))]
    {:fx/type :scroll-pane
     :content {:fx/type :group
               :children (concat
                          (for [h gb]
                            {:fx/type draw-hex
                             :hex h
                             :layout layout})
                          ;; TODO Remove Units which haven't been placed
                          tokens
                          )}}))

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
                             :on-action {:event-type ::events/auto-save :fx/sync true}}]}]}
    ))

(defn unit-stat-block [{:keys [fx/context unit]}]
  (let [background (if (= unit (fx/sub-val context :active-unit))
                     "-fx-background-color: #AAAAAA;"
                     "-fx-background-color: #DDDDDD;")]
    {:fx/type :v-box
     :border {:strokes [{:stroke :black :style :solid :widths 2}]}
     :style background
     :padding 5
     :spacing 5
     :on-mouse-clicked {:event-type ::events/stats-clicked :fx/sync true :unit unit}
     :children [{:fx/type :h-box
                :spacing 5
                :children [{:fx/type common/prop-label
                            :label "Unit: "
                            :value (:id unit)}
                           {:fx/type common/prop-label
                            :label "Type: "
                            :value (:type unit)}
                           {:fx/type common/prop-label
                            :label "Mv: "
                            :value (cu/print-movement unit)}]}
                {:fx/type :h-box
                 :spacing 5
                 :children [{:fx/type common/prop-label
                             :label "Role: "
                             :value (:role unit)}
                            {:fx/type common/prop-label
                             :label "Size: "
                             :value (str (:size unit))}
                            {:fx/type common/prop-label
                             :label "TMM: "
                             :value (str (:tmm unit))}]}
                {:fx/type common/prop-label
                 :label "Pilot (skill): "
                 :value (str (:name (:pilot unit)) " (" (:skill (:pilot unit)) ")")}
                {:fx/type :v-box
                 :spacing 5
                 :children [{:fx/type :label
                             :text "Attacks"}
                            {:fx/type :h-box
                             :children [{:fx/type :v-box
                                         :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                                         :padding {:left 5 :right 5}
                                         :children [{:fx/type :label
                                                     :text "S(+0)"}
                                                    {:fx/type :label
                                                     :text (cu/print-short unit)}]}
                                        {:fx/type :v-box
                                         :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                                         :padding {:left 5 :right 5}
                                         :children [{:fx/type :label
                                                     :text "M(+2)"}
                                                    {:fx/type :label
                                                     :text (cu/print-medium unit)}]}
                                        {:fx/type :v-box
                                         :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                                         :padding {:left 5 :right 5}
                                         :children [{:fx/type :label
                                                     :text "L(+4)"}
                                                    {:fx/type :label
                                                     :text (cu/print-long unit)}]}
                                        {:fx/type :v-box
                                         :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                                         :padding {:left 5 :right 5}
                                         :children [{:fx/type :label
                                                     :text "E(+6)"}
                                                    {:fx/type :label
                                                     :text (cu/print-extreme unit)}]}]}]}
                {:fx/type :v-box
                 :spacing 3
                 :children [{:fx/type :label
                             :text (str "Armor: " (:current-armor unit) "/" (:armor unit))}
                            {:fx/type :h-box
                             :spacing 5
                             :children (concat (for [a (range (:armor unit))]
                                                 (if (< a (:current-armor unit))
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :green}
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :transparent})))}]}
                {:fx/type :v-box
                 :spacing 3
                 :children [{:fx/type :label
                             :text (str "Structure: " (:current-structure unit) "/" (:structure unit))}
                            {:fx/type :h-box
                             :spacing 5
                             :children (concat (for [a (range (:structure unit))]
                                                 (if (< a (:current-structure unit))
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :green}
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :transparent})))}]}
                {:fx/type :v-box
                 :spacing 3
                 :children [{:fx/type :label
                             :text (str "Heat: " (:current-heat unit) "/" 4)}
                            {:fx/type :h-box
                             :spacing 5
                             :children (concat (for [a (range 4)]
                                                 (if (< a (:current-heat unit))
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :red}
                                                   {:fx/type :rectangle
                                                    :x 0 :y 0
                                                    :width 20 :height 10
                                                    :stroke :black
                                                    :fill :aliceblue})))}]}]}))

(defn stat-blocks [{:keys [fx/context]}]
  {:fx/type :scroll-pane
   :min-width :use-pref-size
   :content {:fx/type :v-box
             :spacing 15
             :children (for [u (fx/sub-val context :units)]
                         {:fx/type unit-stat-block :unit u})}})

(defn game-view [{:keys [fx/context]}]
  {:fx/type :grid-pane
   :children [{:fx/type game-board
               :grid-pane/row 0
               :grid-pane/column 0}
              {:fx/type command-palette
               :grid-pane/row 1
               :grid-pane/column 0
               :grid-pane/column-span 2
               :grid-pane/hgrow :always
               :grid-pane/vgrow :always
               }
              {:fx/type stat-blocks
               :grid-pane/row 0
               :grid-pane/column 1
               :grid-pane/hgrow :always
               :grid-pane/vgrow :always}
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
        (= view :game) {:fx/type game-view}
        :else lobby/view)}}))
