(ns megastrike.gui.views
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.table-view :as tables]
   [megastrike.gui.events :as events]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.subs :as sub]))

(defn game-view
  []
  "")

(defn launch-game
  []
  (let [units? (fx/sub-ctx fx/context sub/units-ready?)
        forces? (fx/sub-ctx fx/context sub/forces-ready?)]
    (when (and units? forces?)
      (game-view))))

(defn text-input
  [{:keys [fx/context label key]}]
  {:fx/type :h-box
   :spacing 5
   :children [{:fx/type :label :text label}
              {:fx/type :text-field
               :on-text-changed {:event-type ::events/text-input
                                 :fx/sync true
                                 :key key}
               :text (fx/sub-val context key)}]})

(defn filter-button
  [{:keys [field values text]}]
  {:fx/type :button
   :text text
   :on-action {:event-type ::events/filter-changed
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
              :on-selected-item-changed {:event-type ::events/mul-selection-changed :fx/sync true}
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
                                      :describe (fn [x] {:text (cu/print-short x)})}}
                       {:fx/type :table-column
                       :text "M"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-medium x)})}}
                       {:fx/type :table-column
                       :text "L"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-long x)})}}
                       {:fx/type :table-column
                       :text "E"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-extreme x)})}}
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

(def new-unit-buttons
  {:fx/type :h-box
   :spacing 5
   :alignment :top-center
   :children [{:fx/type text-input
               :label "Pilot Name"
               :key :pilot-name}
              {:fx/type text-input
               :label "Pilot Skill"
               :key :pilot-skill}
              {:fx/type :button
               :text "Add Unit"
               :on-action {:event-type ::events/add-unit :fx/sync true}}]})

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
              {:fx/type mul-table}
              new-unit-buttons]})

(defn forces-table
  [{:keys [fx/context]}]
  (let [forces (fx/sub-val context :forces)
        selected (fx/sub-val context :active-force)
        counts (fx/sub-ctx context sub/unit-counts)]
    (if (empty? forces)
      {:fx/type :label
       :text "Add a force."}
      {:fx/type tables/with-selection-props
       :props {:selection-mode :single
               :on-selected-item-changed {:event-type ::events/force-selection-changed}
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
                                         :describe (fn [x] {:text (:name x)})}}]
              :items (vals forces)}})))

(def force-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :alignment :top-center
   :grid-pane/row 0
   :grid-pane/column 1
   :grid-pane/hgrow :always
   :grid-pane/vgrow :always
   :children [{:fx/type :label :text "Forces"}
              {:fx/type text-input
               :label "Force Name"
               :key :force-name}
              {:fx/type text-input
               :label "Force Deployment"
               :key :force-zone}
              {:fx/type :h-box
               :spacing 5
               :children [{:fx/type :label :text "Color:"}
                          {:fx/type :color-picker
                           :on-value-changed {:event-type ::events/color-changed :fx/sync true}
                           :value :gold}]}
              {:fx/type :button
               :text "Add Force"
               :on-action {:event-type ::events/add-force :fx/sync true}}
              {:fx/type forces-table}]})

(defn units-table [{:keys [fx/context]}]
  (let [units (fx/sub-val context :units)
        forces (fx/sub-val context :forces)
        selected nil]
    {:fx/type tables/with-selection-props
     :props {:selection-mode :single
             :on-selected-item-changed {:event-type ::events/unit-selection-changed :fx/sync true}
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
                                      :describe (fn [x] {:graphic {:fx/type :blend
                                                                   :bottom-input {:fx/type :image-viewer
                                                                                  :image (cu/find-sprite x)}
                                                                   :mode :multiply
                                                                   :top-input (:color (find forces (:force x)))}})}}]
            :items units}}))

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
              {:fx/type text-input
               :label "Map Width"
               :key :map-width}
              {:fx/type text-input
               :label "Map Height"
               :key :map-height}
              {:fx/type :button
               :text "Launch Game"
               :on-action (launch-game)}]})

(def lobby
  {:fx/type :grid-pane
   :children [mul-pane
              force-pane
              unit-pane
              map-pane]})

(defn root [_]
  {:fx/type :stage
   :showing true
   :scene
   {:fx/type :scene
    :root
    lobby}})
