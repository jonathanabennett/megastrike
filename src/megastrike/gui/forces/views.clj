(ns megastrike.gui.forces.views
  (:require
   [cljfx.api :as fx]
   [clojure.string :as str]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.common :as common]
   [megastrike.gui.events :as events]))

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
   :border {:strokes [{:stroke (:color (:force (first units))) :style :solid :widths 2}]}
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

