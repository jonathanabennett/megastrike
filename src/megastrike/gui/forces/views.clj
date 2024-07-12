(ns megastrike.gui.forces.views
  (:require [clojure.string :as str]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.common :as common]
            [megastrike.gui.subs :as subs]
            [megastrike.gui.events :as events]))

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
                                                     :text (cu/print-extreme unit)}]}]}]})

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
(defn unit-stat-block [{:keys [fx/context unit]}]
  (let [active (subs/active-id context) ]
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
                                       :value (str (cu/get-tmm unit))}]} 
                          {:fx/type common/prop-label 
                           :label "Pilot (skill): " 
                           :value (str (:name (:pilot unit)) " (" (:skill (:pilot unit)) ")")} 
                          {:fx/type attack-table 
                           :unit unit} 
                          { :fx/type draw-pips 
                           :text (str "Armor: " (:current-armor unit) "/" (:armor unit)) 
                           :filled (:current-armor unit) 
                           :max (:armor unit) 
                           :fill-one :green 
                           :fill-two :transparent} 
                          {:fx/type draw-pips 
                           :text (str "Structure: " (:current-structure unit) "/" (:structure unit)) 
                           :filled (:current-structure unit) 
                           :max (:structure unit) 
                           :fill-one :green 
                           :fill-two :transparent} 
                          {:fx/type draw-pips 
                           :text (str "Heat: " (:current-heat unit) "/" 4) 
                           :filled (:current-heat unit) 
                           :max 4 
                           :fill-one :red 
                           :fill-two :aliceblue}
                          {:fx/type common/prop-label
                           :label "Abilities: "
                           :value (str (:abilities unit))}
                          {:fx/type common/prop-label
                           :label "Criticals: "
                           :value (str (:crits unit))}]}}))

(defn force-block [{:keys [fx/context units]}]
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

(defn stat-blocks [{:keys [fx/context]}]
  (let [units (group-by :force (vals (subs/units context)))]
    {:fx/type :scroll-pane
     :min-width :use-pref-size
     :content {:fx/type :v-box
               :spacing 30
               :children (for [force units]
                           {:fx/type force-block :units (val force)})}}))
