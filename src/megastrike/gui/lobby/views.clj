(ns megastrike.gui.lobby.views
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.table-view :as tables]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.elements :as elements]
   [megastrike.gui.lobby.events :as lobby-events]
   [megastrike.gui.subs :as sub]
   [megastrike.utils :as utils]))

(defn filter-button
  [{:keys [field values text]}]
  {:fx/type :button
   :text text
   :on-action {:event-type ::lobby-events/filter-changed
               :fx/sync true
               :field field
               :values values}})

(def mul-filter-buttons
  {:fx/type :h-box
   :spacing 5
   :alignment :top-center
   :children [{:fx/type filter-button
               :field :type
               :values cu/ground-units
               :text "All Ground Units"}
              {:fx/type filter-button
               :field :type
               :values cu/bm-units
               :text "Battlemechs"}
              {:fx/type filter-button
               :field :type
               :values cu/mech-units
               :text "All Mechs"}
              {:fx/type filter-button
               :field :type
               :values cu/conventional-units
               :text "All Conventional Units"}
              {:fx/type filter-button
               :field :type
               :values cu/vehicle-units
               :text "All vehicles"}
              {:fx/type filter-button
               :field :type
               :values cu/infantry-units
               :text "All Infantry"}]})

(defn mul-table [{:keys [fx/context]}]
  (let [mul (fx/sub-val context :mul)
        selected (fx/sub-val context :active-mul)]
    {:fx/type tables/with-selection-props
     :props {:selection-mode :single
             :on-selected-item-changed {:event-type ::lobby-events/mul-selection-changed :fx/sync true}
             :selected-item selected}
     :desc {:fx/type :table-view
            :columns [{:fx/type :table-column
                       :text "Unit Name"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (:full-name x)})}}
                      {:fx/type :table-column
                       :text "Type"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (:type x)})}}
                      {:fx/type :table-column
                       :text "PV"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:point-value x))})}}
                      {:fx/type :table-column
                       :text "Size"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:size x))})}}
                      {:fx/type :table-column
                       :text "Movement"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-movement x)})}}
                      {:fx/type :table-column
                       :text "TMM"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:tmm x))})}}
                      {:fx/type :table-column
                       :text "Armor"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:armor x))})}}
                      {:fx/type :table-column
                       :text "Structure"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:structure x))})}}
                ;; {:fx/type :table-column
                ;;  :text "Threshold"
                ;;  :cell-value-factory identity
                ;;  :cell-factory {:fx/cell-type :table-cell
                ;;                 :describe (fn [x] {:text (pr-str (:threshold x))})}}
                      {:fx/type :table-column
                       :text "S"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage-bracket x :s)})}}
                      {:fx/type :table-column
                       :text "M"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage-bracket x :m)})}}
                      {:fx/type :table-column
                       :text "L"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage-bracket x :l)})}}
                      {:fx/type :table-column
                       :text "E"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage-bracket x :e)})}}
                      {:fx/type :table-column
                       :text "OV"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:overheat x))})}}
                      {:fx/type :table-column
                       :text "Abilities"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (:abilities x)})}}]
            :items mul}}))

(def mul-chassis-search
  {:fx/type :h-box
   :spacing 5
   :alignment :top-center
   :children [{:fx/type elements/text-input
               :label "Search:"
               :key :mul-search-term}
              {:fx/type :button
               :text "Search by name"
               :on-action {:event-type ::lobby-events/filter-mul :fx/sync true :field :full-name}}]})

(def new-unit-buttons
  {:fx/type :h-box
   :spacing 5
   :alignment :top-center
   :children [{:fx/type elements/text-input
               :label "Pilot Name"
               :key :pilot-name}
              {:fx/type elements/text-input
               :label "Pilot Skill"
               :key :pilot-skill}
              {:fx/type :button
               :text "Add Unit"
               :on-action {:event-type ::lobby-events/add-unit :fx/sync true}}]})

(def mul-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :alignment :top-center
   :grid-pane/row 0
   :grid-pane/column 0
   :grid-pane/hgrow :always
   :grid-pane/vgrow :always
   :children [{:fx/type :label
               :text "Master Unit List"}
              mul-filter-buttons
              mul-chassis-search
              {:fx/type mul-table}
              new-unit-buttons]})

(defn forces-table
  [{:keys [fx/context]}]
  (let [forces (fx/sub-val context :forces)
        selected (fx/sub-val context :active-force)
        counts (fx/sub-ctx context sub/units-by-force)]
    (if (empty? forces)
      {:fx/type :label
       :text "Add a force."}
      {:fx/type tables/with-selection-props
       :props {:selection-mode :single
               :on-selected-item-changed {:event-type ::lobby-events/force-selection-changed}
               :selected-item selected}
       :desc {:fx/type :table-view
              :row-factory {:fx/cell-type :table-row
                            :describe (fn [x]
                                        {:style {:-fx-border-color (or (:color x) :black)}})}
              :columns [{:fx/type :table-column
                         :text "Name"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (:name x)})}}
                        {:fx/type :table-column
                         :text "Deployment"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (:deploy x)})}}
                        {:fx/type :table-column
                         :text "Unit Count"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (prn-str (count ((utils/keyword-maker (:name x)) counts)))})}}
                        {:fx/type :table-column
                         :text "Total PV"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (prn-str (reduce + (map #(cu/pv %) ((utils/keyword-maker (:name x)) counts))))})}}]
              :items (vals forces)}})))

(defn force-pane
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :alignment :top-center
   :children [{:fx/type :label :text "Forces"}
              {:fx/type elements/text-input
               :label "Force Name"
               :key :force-name}
              {:fx/type elements/text-input
               :label "Force Deployment"
               :key :force-zone}
              {:fx/type :h-box
               :spacing 5
               :children [{:fx/type :label :text "Color:"}
                          {:fx/type :color-picker
                           :on-value-changed {:event-type ::lobby-events/color-changed :fx/sync true}
                           :value :gold}]}
              (if (fx/sub-val context :force-camo)
                {:fx/type :button
                 :background {:images (list (fx/sub-val context :force-camo))}
                 :text "Change Camo"
                 :on-action {:event-type ::lobby-events/select-camo :fx/sync true}}
                {:fx/type :button
                 :text "Select Camo"
                 :on-action {:event-type ::lobby-events/select-camo :fx/sync true}})
              {:fx/type :button
               :text "Add Force"
               :on-action {:event-type ::lobby-events/add-force :fx/sync true}}
              {:fx/type forces-table}]})

(defn units-table [{:keys [fx/context]}]
  (let [units (fx/sub-val context :units)
        forces (fx/sub-val context :forces)
        selected nil]
    (if (empty? units)
      {:fx/type :label
       :text "Please add a unit."}
      {:fx/type tables/with-selection-props
       :props {:selection-mode :single
               :on-selected-item-changed {:event-type ::lobby-events/unit-selection-changed :fx/sync true}
               :selected-item selected}
       :desc {:fx/type :table-view
              :columns [{:fx/type :table-column
                         :text "Unit"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (:id x)})}}
                        {:fx/type :table-column
                         :text "Image"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:graphic {:fx/type elements/draw-sprite
                                                                     :unit x
                                                                     :force ((:force x) forces)
                                                                     :x 0
                                                                     :y 0
                                                                     :shift 0}})}}
                        {:fx/type :table-column
                         :text "Pilot"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (str (:name (:pilot x)) " (" (:skill (:pilot x)) ")")})}}
                        {:fx/type :table-column
                         :text "PV"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (prn-str (cu/pv x))})}}]
              :items (vals units)}})))

(def unit-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :alignment :top-center
   :grid-pane/row 1
   :grid-pane/column 0
   :grid-pane/hgrow :always
   :grid-pane/vgrow :always
   :children [{:fx/type :label
               :text "Unit List"}
              {:fx/type units-table}]})

(defn map-grid
  [{:keys [fx/context]}]
  (let [width (Integer/parseInt (fx/sub-val context :map-width))
        height (Integer/parseInt (fx/sub-val context :map-height))
        boards (fx/sub-val context :map-boards)]
    {:fx/type :grid-pane
     :children (for [x (range width)
                     y (range height)]
                 {:fx/type :button
                  :grid-pane/column x
                  :grid-pane/row y
                  :text (:name (nth boards (+ (* y width) x) {:name "None"}))
                  :id (str (+ (* y width) x))
                  :on-action {:event-type ::lobby-events/load-mapboard :id (+ (* y width) x)}})}))

(def map-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :alignment :top-center
   :grid-pane/row 1
   :grid-pane/column 1
   :grid-pane/hgrow :always
   :grid-pane/vgrow :always
   :children [{:fx/type :label
               :text "Map Setup"}
              {:fx/type elements/text-input
               :label "Map Width (in boards)"
               :key :map-width}
              {:fx/type elements/text-input
               :label "Map Height (in boards)"
               :key :map-height}
              {:fx/type map-grid}
              {:fx/type :button
               :text "Load Test Game"
               :on-action {:event-type ::lobby-events/load-save :fx/sync true}}
              {:fx/type :button
               :text "Load Scenario"
               :on-action {:event-type ::lobby-events/load-scenario :fx/sync true}}
              {:fx/type :button
               :text "Launch Game"
               :on-action {:event-type ::lobby-events/launch-game :fx/sync true :view :game}}]})

