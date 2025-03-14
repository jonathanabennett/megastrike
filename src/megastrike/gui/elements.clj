(ns megastrike.gui.elements
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.combat-unit :as cu]
   [megastrike.force :as force]
   [megastrike.gui.events :as events]
   [megastrike.gui.subs :as subs]
   [megastrike.hexagons.hex :as hex]
   [megastrike.mul :as mul]))

;; Common GUI Widgets
(defn prop-label
  "Creates a text-flow, which contains a label and a value tied to that label."
  [{:keys [label value]}]
  {:fx/type :text-flow
   :children [{:fx/type :text
               :style "-fx-font-weight: bold;"
               :text label}
              {:fx/type :text
               :style "-fx-font-size: 14"
               :text value}]})

(defn text-input
  "Helper method to create a text input box which automatically updates the atom as the text is edited."
  [{:keys [fx/context label key]}]
  {:fx/type :h-box
   :spacing 5
   :children [{:fx/type :label :text label}
              {:fx/type :text-field
               :on-text-changed {:event-type ::events/text-input
                                 :fx/sync true
                                 :key key}
               :text (fx/sub-val context key)}]})

(defn confirmation-pane
  [{:keys [fx/context dialog-id on-confirmed button dialog-pane]}]
  {:fx/type fx/ext-let-refs
   :refs {::dialog {:fx/type :dialog
                    :showing (fx/sub-val context get-in [:internal dialog-id :showing] false)
                    :on-hidden {:event-type ::events/on-confirmation-dialog-hidden
                                :dialog-id dialog-id
                                :on-confirmed on-confirmed}
                    :dialog-pane (merge {:fx/type :dialog-pane
                                         :button-types [:cancel :ok]}
                                        dialog-pane)}}
   :desc (merge {:fx/type :button
                 :on-action {:event-type ::events/show-confirmation
                             :dialog-id dialog-id}}
                button)})

;; Sprites
(defn draw-sprite
  "Draws a sprite. Used for both the map and the lobby."
  [{:keys [unit force x y shift direction]}]
  (let [camo (force/get-camo force)
        color "#FFFFFF"
        img (mul/find-sprite unit)]
    {:fx/type :image-view
     :image (str "file:" (.getPath img))
     :effect {:fx/type :blend
              :top-input (if camo
                           {:fx/type :image-input
                            :source camo}
                           {:fx/type :color-input
                            :paint color
                            :x 0 :y 0 :width 100 :height 100})
              :bottom-input {:fx/type :image-input
                             :source (str (.toURI (mul/find-sprite unit)))}
              :mode :src-atop
              :opacity 0.5}
     :rotate (if direction
               (:angle direction)
               0)
     :translate-x x
     :translate-y (+ y shift)
     :x 0
     :y 0}))

;; Boards
(defn draw-hex
  [{:keys [hex layout]}]
  (let [points (hex/points hex layout)
        offset (hex/hex->offset hex)
        {:keys [elevation terrain]} hex
        ;; Colors below come from data/images/hexes/defaultminimap.txt
        sprite (cond
                 (str/includes? terrain "woods") "rgb(180, 230, 130)"
                 (str/includes? terrain "sinkhole") "rgb(210, 180, 150)"
                 (str/includes? terrain "rough") "rgb(186, 191, 160)"
                 (str/includes? terrain "rubble") "rgb(200, 200, 200)"
                 (str/includes? terrain "water") "rgb(200, 247, 253)"
                 (str/includes? terrain "pavement") "rgb(204, 204, 204)"
                 (str/includes? terrain "road") "rgb(71, 79, 107)"
                 (str/includes? terrain "swamp") "rgb(49, 136, 74)"
                 (str/includes? terrain "building") "rgb(204, 204, 204)"
                 (str/includes? terrain "bridge") "rgb(109, 55, 25)"
                 :else "rgb(215, 211, 156)")]
    {:fx/type :group
     :on-mouse-clicked {:event-type ::events/hex-clicked :hex hex :fx/sync true}
     :children [{:fx/type :polygon
                 :points points
                 :fill sprite
                 :stroke :black}
                {:fx/type :label
                 :text (format "Lvl %s %s" elevation (first (str/split terrain #":")))
                 :layout-x (nth points 4)
                 :layout-y (nth points 5)
                 :font 16
                 :translate-x (* 10 (:scale layout))
                 :translate-y (* -20 (:scale layout))}
                {:fx/type :label
                 :text (format "%02d%02d" (:x offset) (:y offset))
                 :layout-x (nth points 8)
                 :layout-y (nth points 9)
                 :font 16
                 :translate-x (* 10 (:scale layout))
                 :translate-y (* 10 (:scale layout))}]}))

(defn draw-unit
  [{:keys [fx/context unit layout]}]
  (let [hex (hex/points (cu/get-location unit) layout)
        forces (subs/forces context)
        force (forces (unit :force))]
    {:fx/type :group
     :on-mouse-clicked {:event-type ::events/unit-clicked :unit unit :fx/sync true}
     :children [{:fx/type draw-sprite
                 :unit unit
                 :force force
                 :x (nth hex 8)
                 :y (nth hex 9)
                 :direction (cu/get-facing unit)
                 :shift (/ (* (layout :y-size) (:scale layout)) 3)}
                {:fx/type :label
                 :text (unit :full-name)
                 :layout-x (nth hex 8)
                 :layout-y (nth hex 9)
                 :font 16
                 :translate-y (/ (* (layout :y-size) (:scale layout)) 3)}
                {:fx/type :label
                 :text (if (cu/get-selected-movement unit false)
                         (name (cu/get-selected-movement unit false))
                         "Did not move")
                 :layout-x (nth hex 4)
                 :layout-y (nth hex 5)
                 :font 16
                 :translate-y (* (/ (* (layout :y-size) (:scale layout)) 3) -2)}]}))

(defn draw-movement-cost
  [{:keys [origin destination layout cost]}]
  (let [origin-pixel (hex/hex->pixel origin layout)
        dest-pixel (hex/hex->pixel destination layout)]
    {:fx/type :group
     :children [{:fx/type :line
                 :start-x (:x origin-pixel)
                 :start-y (:y origin-pixel)
                 :end-x (:x dest-pixel)
                 :end-y (:y dest-pixel)}
                {:fx/type :label
                 :text (str cost)
                 :layout-x (:x dest-pixel)
                 :layout-y (:y dest-pixel)
                 :font 16}]}))

(defn draw-movement-path
  [{:keys [unit layout]}]
  (let [origin (cu/get-location unit)
        costs (cu/get-movement-cost unit)]
    {:fx/type :group
     :children (loop [total 0
                      costs costs
                      o origin
                      sprites []
                      path (cu/get-path unit)]
                 (if (empty? path)
                   sprites
                   (recur (+ total (or (first costs) 0))
                          (rest costs)
                          (first path)
                          (into [] (concat sprites
                                           [{:fx/type draw-movement-cost
                                             :origin o
                                             :destination (first path)
                                             :layout layout
                                             :cost total}]))
                          (rest path))))}))

;; Forces Lists
(defn attack-table
  "Helper Method that generates the attack table used in a stat block."
  [{:keys [unit]}]
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
                                       :text (cu/print-damage unit :s)}]}
                          {:fx/type :v-box
                           :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                           :padding {:left 5 :right 5}
                           :children [{:fx/type :label
                                       :text "M(+2)"}
                                      {:fx/type :label
                                       :text (cu/print-damage unit :m)}]}
                          {:fx/type :v-box
                           :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                           :padding {:left 5 :right 5}
                           :children [{:fx/type :label
                                       :text "L(+4)"}
                                      {:fx/type :label
                                       :text (cu/print-damage unit :l)}]}
                          {:fx/type :v-box
                           :border {:strokes [{:stroke :black :style :solid :widths 1}]}
                           :padding {:left 5 :right 5}
                           :children [{:fx/type :label
                                       :text "E(+6)"}
                                      {:fx/type :label
                                       :text (cu/print-damage unit :e)}]}]}]})

(defn draw-pips
  "Helper Function for drawing a series of pips."
  [{:keys [filled max text fill-one fill-two]}]
  {:fx/type :v-box
   :spacing 3
   :children [{:fx/type :label
               :text text}
              {:fx/type :h-box
               :spacing 5
               :children (concat (for [a (range max)]
                                   (if (< a filled)
                                     {:fx/type :rectangle
                                      :x 0 :y 0
                                      :width 20 :height 10
                                      :stroke :black
                                      :fill fill-one}
                                     {:fx/type :rectangle
                                      :x 0 :y 0
                                      :width 20 :height 10
                                      :stroke :black
                                      :fill fill-two})))}]})

(defn unit-stat-block
  [{:keys [fx/context unit]}]
  (let [active (subs/active-id context)]
    {:fx/type :titled-pane
     :on-mouse-clicked {:event-type ::events/stats-clicked :fx/sync true :unit (:id unit)}
     :text (if (:acted unit) (str (:id unit) " (done)") (:id unit))
     :border {:strokes [{:stroke :black :style :solid :width 2}]}
     :padding 5
     :content {:fx/type :v-box
               :style (cond
                        (= (:id unit) active) "-fx-background-color: #BBBBBB;"
                        (not (:acted unit)) "-fx-background-color: 999999"
                        :else "-fx-background-color: #DDDDDD;")
               :spacing 5
               :children [{:fx/type :h-box
                           :spacing 5
                           :children [{:fx/type prop-label
                                       :label "Unit: "
                                       :value (:id unit)}
                                      {:fx/type prop-label
                                       :label "Force: "
                                       :value (-> unit :force name str/capitalize)}
                                      {:fx/type prop-label
                                       :label "Type: "
                                       :value (:type unit)}
                                      {:fx/type prop-label
                                       :label "Mv: "
                                       :value (cu/print-movement unit)}]}
                          {:fx/type :h-box
                           :spacing 5
                           :children [{:fx/type prop-label
                                       :label "Role: "
                                       :value (:role unit)}
                                      {:fx/type prop-label
                                       :label "Size: "
                                       :value (str (cu/get-size unit))}
                                      {:fx/type prop-label
                                       :label "TMM: "
                                       :value (str (cu/tmm unit))}]}
                          {:fx/type prop-label
                           :label "Pilot (skill): "
                           :value (cu/display-pilot unit)}
                          {:fx/type attack-table
                           :unit unit}
                          {:fx/type draw-pips
                           :text (str "Armor: " (cu/get-current unit :armor) "/" (cu/get-max unit :armor))
                           :filled (cu/get-current unit :armor)
                           :max (cu/get-max unit :armor)
                           :fill-one :green
                           :fill-two :transparent}
                          {:fx/type draw-pips
                           :text (str "Structure: " (cu/get-current unit :structure) "/" (cu/get-max unit :structure))
                           :filled (cu/get-current unit :structure)
                           :max (cu/get-max unit :structure)
                           :fill-one :green
                           :fill-two :transparent}
                          {:fx/type draw-pips
                           :text (str "Heat: " (cu/get-heat unit) "/" 4)
                           :filled (cu/get-heat unit)
                           :max 4
                           :fill-one :red
                           :fill-two :aliceblue}
                          {:fx/type prop-label
                           :label "Abilities: "
                           :value (cu/print-abilities unit)}
                          {:fx/type prop-label
                           :label "Criticals: "
                           :value (cu/get-crits unit)}
                          {:fx/type draw-pips
                           :text (str "Remaining Armor: " (cu/get-remaining-armor unit) "/" (cu/get-max unit :armor))
                           :filled (cu/get-remaining-armor unit)
                           :max (cu/get-max unit :armor)
                           :fill-one :green
                           :fill-two :transparent}
                          {:fx/type draw-pips
                           :text (str "Remaining structure " (cu/get-remaining-structure unit) "/" (cu/get-max unit :structure))
                           :filled (cu/get-remaining-structure unit)
                           :max (cu/get-max unit :structure)
                           :fill-one :green
                           :fill-two :transparent}
                          {:fx/type prop-label
                           :label "Unapplied Criticals: "
                           :value (str (cu/get-new-crits unit))}]}}))

(defn force-block
  [{:keys [fx/context units]}]
  (let [forces (subs/forces context)
        force ((:force (first units)) forces)]
    {:fx/type :v-box
     :spacing 5
     :border {:strokes [{:stroke (:color force) :style :solid :width 5}]}
     :children [{:fx/type :label
                 :text (force :name)}
                {:fx/type :accordion
                 :panes (for [u units]
                          {:fx/type unit-stat-block :unit u})}]}))

(defn stat-blocks
  [{:keys [fx/context]}]
  (let [units (group-by :force (vals (subs/units context)))]
    {:fx/type :scroll-pane
     :min-width :use-pref-size
     :content {:fx/type :v-box
               :spacing 30
               :children (for [force units]
                           {:fx/type force-block :units (val force)})}}))

;; Button Widgets
(defn attack-buttons
  [attacks unit phase mv-type]
  (loop [ret [{:fx/type :button
               :text "No Attack"
               :on-action {:event-type ::events/close-attack-selection :selected false :unit unit}}]
         attacks attacks]
    (if (empty? attacks)
      ret
      (recur (let [atk-data (second (first attacks))
                   range (:range atk-data)
                   attack (:attack atk-data)
                   disabled? (cond
                               (and (= phase :movement) (= mv-type :jump)) (not= :dfa attack)
                               (= phase :movement) (not= :charge attack)
                               (and (= phase :combat) (not= range 1)) (contains? #{:physical :charge :dfa} attack)
                               :else (contains? #{:charge :dfa} attack))]
               ((comp vec flatten conj) ret {:fx/type :button
                                             :text (str (name (:attack atk-data)) ": " (cu/print-attack-roll atk-data false))
                                             :disable disabled?
                                             :on-action {:event-type ::events/close-attack-selection  :unit unit :selected atk-data :fx/sync true}}))
             (rest attacks)))))

(defn deploy-buttons
  [finished-deployment]
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

(defn move-buttons
  [unit]
  (let [movement (cu/get-movement-modes unit)
        buttons (if (contains? movement :jump)
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
       :on-action {:event-type ::events/confirm-move :fx/sync true}}])))

(def combat-phase-buttons
  [{:fx/type :button
    :text "Overheat +1"
    :on-action {:event-type ::events/overheat :value 1 :fx/sync true}}
   {:fx/type :button
    :text "Overheat -1"
    :on-action {:event-type ::events/overheat :value -1 :fx/sync true}}
   {:fx/type :button
    :text "Auto-attack"
    :on-action {:event-type ::events/auto-attack :fx/sync true}}
   {:fx/type :button
    :text "Resolve Charges/DFAs"
    :on-action {:event-type ::events/resolve-physicals :fx/sync true}}
   {:fx/type :button
    :text "Finish Attacks"
    :on-action {:event-type ::events/finish-attacks :fx/sync true}}])

(defn command-palette
  [{:keys [fx/context]}]
  (let [phase (subs/phase context)
        turn (subs/turn-number context)
        turn-order (subs/turn-order context)
        unit (subs/active-unit context)
        common-buttons [{:fx/type :separator
                         :orientation :vertical
                         :padding 15}
                        {:fx/type :button
                         :text "Next Phase"
                         :on-action {:event-type ::events/next-phase}
                         :disable #_{:clj-kondo/ignore [:not-empty?]}
                         (not (empty? turn-order))}
                        {:fx/type :button
                         :text "Round Report"
                         :on-action {:event-type ::events/open-round-dialog}}
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
                        (= phase :combat) combat-phase-buttons

                        :else [])
        buttons ((comp vec flatten vector) phase-buttons common-buttons)]
    {:fx/type :v-box
     :spacing 5
     :children [{:fx/type :label
                 :text (str (str/capitalize (name phase)) " Phase | Turn " turn " | " (prn-str turn-order))}
                {:fx/type :h-box
                 :children buttons}]}))

