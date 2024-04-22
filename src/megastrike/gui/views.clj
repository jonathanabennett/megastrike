(ns megastrike.gui.views
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.common :as common]
   [megastrike.gui.events :as events]
   [megastrike.gui.lobby.views :as lobby]
   [megastrike.hexagons.hex :as hex]))

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
                 :translate-y -20}]}))

(defn draw-unit [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points unit layout)
        forces (fx/sub-val context :forces)
        force (forces (unit :force))] 
    {:fx/type :group
     :on-mouse-clicked {:event-type ::events/unit-clicked :unit unit}
     :children [{:fx/type common/draw-sprite 
                 :unit unit 
                 :force force 
                 :x (nth hex 8)
                 :y (nth hex 9)
                 :shift (/ (layout :y-size) 3)}
                {:fx/type :label
                 :text (unit :full-name)
                 :layout-x (nth hex 8)
                 :layout-y (nth hex 9)
                 :font 16
                 :translate-y (/ (layout :y-size) 3)}]}))

(defn game-board [{:keys [fx/context]}]
  (let [gb (fx/sub-val context :game-board)
        layout (fx/sub-val context :layout)
        units (vals (fx/sub-val context :units))
        tokens (loop [i 0
                      tokens []]
                 (if (= (count units) i)
                   tokens
                   (recur
                    (inc i)
                    (if (:q (nth units i))
                      (conj tokens (nth units i))
                      tokens))))] 
    {:fx/type :scroll-pane
     :content {:fx/type :group
               :children (concat
                          (for [h gb]
                            {:fx/type draw-hex
                             :hex h
                             :layout layout})
                          (for [t tokens]
                            {:fx/type draw-unit
                             :unit t
                             :layout layout }))}}))

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

(defn unit-stat-block [{:keys [fx/context unit]}]
  (let [background (if (= (:id unit) (fx/sub-val context :active-unit))
                     "-fx-background-color: #AAAAAA;"
                     "-fx-background-color: #DDDDDD;")]
    {:fx/type :v-box
     :border {:strokes [{:stroke :black :style :solid :widths 2}]}
     :style background
     :padding 5
     :spacing 5
     :on-mouse-clicked {:event-type ::events/stats-clicked :fx/sync true :unit (:id unit)}
     :children [{:fx/type :h-box
                 :spacing 5
                 :children [{:fx/type common/prop-label
                             :label "Unit: "
                             :value (:id unit)}
                            {:fx/type common/prop-label
                             :label "Force: "
                             :value (-> unit :force name str/capitalize)}
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
                {:fx/type common/attack-table
                 :unit unit}
                { :fx/type common/draw-pips 
                 :text (str "Armor: " (:current-armor unit) "/" (:armor unit)) 
                 :filled (:current-armor unit)
                 :max (:armor unit)
                 :fill-one :green
                 :fill-two :transparent}
                {:fx/type common/draw-pips 
                 :text (str "Structure: " (:current-structure unit) "/" (:structure unit))
                 :filled (:current-structure unit)
                 :max (:structure unit)
                 :fill-one :green
                 :fill-two :transparent}
                {:fx/type common/draw-pips
                 :text (str "Heat: " (:current-heat unit) "/" 4)
                 :filled (:current-heat unit)
                 :max 4
                 :fill-one :red 
                 :fill-two :aliceblue} ]}))

(defn force-block [{:keys [units]}]
  {:fx/type :v-box
   :spacing 15
   :border {:strokes [{:stroke :black :style :solid :widths 2}]}
   :children (for [u units]
               {:fx/type unit-stat-block :unit u})})

(defn stat-blocks [{:keys [fx/context]}]
  (let [units (group-by :force (vals (fx/sub-val context :units)))]
    {:fx/type :scroll-pane
     :min-width :use-pref-size
     :content {:fx/type :v-box
               :spacing 30
               :children (for [force units]
                           {:fx/type force-block :units (val force)})}}))

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
               :grid-pane/vgrow :always}
              {:fx/type stat-blocks
               :grid-pane/row 0
               :grid-pane/column 1
               :grid-pane/hgrow :always
               :grid-pane/vgrow :always}]})

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
