(ns megastrike.gui.lobby.views
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.table-view :as tables]
   [com.brunobonacci.mulog :as mu]
   [megastrike.battle-force :as battle-force]
   [megastrike.combat-unit :as cu]
   [megastrike.gui.elements :as elements]
   [megastrike.gui.lobby.events :as lobby-events]
   [megastrike.gui.subs :as subs]
   [megastrike.movement :as movement]
   [megastrike.pilot :as pilot]))

(defn filter-button
  [{:keys [values text]}]
  {:fx/type :button
   :text text
   :on-action {:event-type ::lobby-events/filter-changed
               :fx/sync true
               :values values}})

(def mul-filter-buttons
  {:fx/type :h-box
   :spacing 5
   :alignment :top-center
   :children [{:fx/type filter-button
               :field :unit/type
               :values :mul/ground-units
               :text "All Ground Units"}
              {:fx/type filter-button
               :field :unit/type
               :values :type/bm
               :text "Battlemechs"}
              {:fx/type filter-button
               :field :unit/type
               :values :mul/mechs
               :text "All Mechs"}
              {:fx/type filter-button
               :field :unit/type
               :values :mul/conventional
               :text "All Conventional Units"}
              {:fx/type filter-button
               :field :unit/type
               :values :mul/vehicle
               :text "All vehicles"}
              {:fx/type filter-button
               :field :unit/type
               :values :mul/infantry
               :text "All Infantry"}]})

(defn name-factory
  [unit]
  {:text (str (:unit/full-name unit))})

(defn movement-factory
  [unit]
  (let [mv-string (movement/print-movement unit)]
    (if (string? mv-string)
      {:text mv-string}
      {:text "No movement"})))

(defn tmm-factory
  [unit]
  (let [tmm-string (str (movement/base-tmm unit))]
    (if (string? tmm-string)
      {:text tmm-string}
      (do (mu/log ::invalid-tmm?
                  :tmm tmm-string)
          {:text "No tmm"}))))

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
                                      :describe name-factory}}
                      {:fx/type :table-column
                       :text "Type"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (:unit/type x))})}}
                      {:fx/type :table-column
                       :text "PV"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (:unit/base-pv x))})}}
                      {:fx/type :table-column
                       :text "Size"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (:unit/size x))})}}
                      {:fx/type :table-column
                       :text "Movement"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe movement-factory}}
                      {:fx/type :table-column
                       :text "TMM"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe tmm-factory}}
                      {:fx/type :table-column
                       :text "Armor"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (get-in x [:unit/armor :toughness/current]))})}}
                      {:fx/type :table-column
                       :text "Structure"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (get-in x [:unit/structure :toughness/current]))})}}
                ;; {:fx/type :table-column
                ;;  :text "Threshold"
                ;;  :cell-value-factory identity
                ;;  :cell-factory {:fx/cell-type :table-cell
                ;;                 :describe (fn [x] {:text (pr-str (:threshold x))})}}
                      {:fx/type :table-column
                       :text "S"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage x :attack/s)})}}
                      {:fx/type :table-column
                       :text "M"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage x :attack/m)})}}
                      {:fx/type :table-column
                       :text "L"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage x :attack/l)})}}
                      {:fx/type :table-column
                       :text "E"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (cu/print-damage x :attack/e)})}}
                      {:fx/type :table-column
                       :text "OV"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (pr-str (:unit/overheat x))})}}
                      {:fx/type :table-column
                       :text "Abilities"
                       :cell-value-factory identity
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x] {:text (str (:unit/abilities x))})}}]
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
               :on-action {:event-type ::lobby-events/filter-mul :fx/sync true :field :unit/full-name}}]})

(defn new-unit-buttons
  [{:keys [fx/context]}]
  (let [selected (fx/sub-val context :active-force)
        battle-force (if selected (get (subs/forces context) selected) nil)]
    {:fx/type :v-box
     :spacing 5
     :alignment :top-center
     :children [{:fx/type :label
                 :text (if battle-force (:unit-group/name battle-force) "")}
                {:fx/type :h-box
                 :spacing 5
                 :alignment :top-center
                 :children [{:fx/type elements/text-input
                             :label "Pilot Name"
                             :key :pilot-name}
                            {:fx/type elements/text-input
                             :label "Pilot Skill"
                             :key :pilot-skill}]}]}))

(def mul-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :children [{:fx/type :label
               :text "Master Unit List"}
              mul-filter-buttons
              mul-chassis-search
              {:fx/type mul-table}
              {:fx/type new-unit-buttons}]})

(defn mul-dialog
  [_]
  {:fx/type elements/confirmation-pane
   :dialog-id :mul-dialog
   :on-confirmed {:event-type ::lobby-events/add-unit}
   :button {:text "Add new unit to selected force"}
   :dialog-pane {:content mul-pane}})

(defn forces-table
  [{:keys [fx/context]}]
  (let [forces (subs/forces context)
        selected (fx/sub-val context :active-force)
        units (subs/units context)
        counts (fx/sub-ctx context subs/units-by-force)]
    (if (empty? forces)
      {:fx/type :label
       :text "Add a force."}
      {:fx/type tables/with-selection-props
       :props {:selection-mode :single
               :on-selected-item-changed {:event-type ::lobby-events/force-selection-changed}
               :selected-item selected}
       :desc {:fx/type :table-view
              :columns [{:fx/type :table-column
                         :text "Name"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (:unit-group/name x)})}}
                        {:fx/type :table-column
                         :text "Player"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (name (or (:unit-group/player x) :none))})}}
                        {:fx/type :table-column
                         :text "Deployment"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (or (name (:unit-group/deployment x)) (name :none))})}}
                        {:fx/type :table-column
                         :text "Unit Count"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (prn-str (or (count (battle-force/force-units x units)) 0))})}}
                        {:fx/type :table-column
                         :text "Total PV"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (prn-str (or (battle-force/force-pv x units) 0))})}}]

              :items (vals forces)}})))

(defn force-creation-dialog
  [{:keys [fx/context]}]
  {:fx/type elements/confirmation-pane
   :dialog-id :force-creation-dialog
   :on-confirmed {:event-type ::lobby-events/add-force}
   :button {:text "Add/Edit force"}
   :dialog-pane {:content {:fx/type :v-box
                           :spacing 5
                           :fill-width true
                           :alignment :top-center
                           :children [{:fx/type :label :text "Add/Edit Force"}
                                      {:fx/type elements/text-input
                                       :label "Force Name"
                                       :key :force-name}
                                      {:fx/type elements/text-input
                                       :label "Force Deployment"
                                       :key :force-zone}
                                      {:fx/type :h-box
                                       :spacing 5
                                       :children [{:fx/type :text :text "Human or AI?"}
                                                  {:fx/type :choice-box
                                                   :items [:player :kevin]
                                                   :value :player
                                                   :on-value-changed {:event-type ::lobby-events/change-player}}]}
                                      (if (fx/sub-val context :force-camo)
                                        {:fx/type :button
                                         :background {:images (list (fx/sub-val context :force-camo))}
                                         :text "Change Camo"
                                         :on-action {:event-type ::lobby-events/select-camo :fx/sync true}}
                                        {:fx/type :button
                                         :text "Select Camo"
                                         :on-action {:event-type ::lobby-events/select-camo :fx/sync true}})]}}})

(def force-pane
  {:fx/type :v-box
   :spacing 5
   :fill-width true
   :grid-pane/row 0
   :grid-pane/column 0
   :grid-pane/hgrow :always
   :grid-pane/vgrow :always
   :alignment :top-center
   :children [{:fx/type force-creation-dialog}
              {:fx/type forces-table}
              {:fx/type mul-dialog}]})

(defn units-table [{:keys [fx/context]}]
  (let [units (subs/units context)
        forces (subs/forces context)
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
                                                                     :bf ((:unit/battle-force x) forces)
                                                                     :x 0
                                                                     :y 0
                                                                     :shift 0}})}}
                        {:fx/type :table-column
                         :text "Pilot"
                         :cell-value-factory identity
                         :cell-factory {:fx/cell-type :table-cell
                                        :describe (fn [x] {:text (pilot/display (:unit/pilot x))})}}
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
   :grid-pane/row 0
   :grid-pane/column 1
   :grid-pane/row-span 2
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
   :grid-pane/column 0
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
              ;; {:fx/type :button
              ;;  :text "Load Test Game"
              ;;  :on-action {:event-type ::lobby-events/load-save :fx/sync true}}
              {:fx/type :button
               :text "Load Scenario"
               :on-action {:event-type ::lobby-events/load-scenario :fx/sync true}}
              {:fx/type :button
               :text "Launch Game"
               :on-action {:event-type ::lobby-events/launch-game :fx/sync true :view :game}}]})
