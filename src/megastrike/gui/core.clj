(ns megastrike.gui.core
  (:require
   [cljfx.api :as fx]
   [megastrike.combat-unit :as cu]))

(def *state
  (atom {:mul cu/mul}))

(def mul-display
  [{:fx/type :label
    :text "Master Unit List"}
   {:fx/type :h-box
    :spacing 5
    :children [{:fx/type :button

               :text "All Ground Units"}
              {:fx/type :button

               :text "Battlemechs"}
              {:fx/type :button

               :text "All Mechs"}
              {:fx/type :button

               :text "All Conventional Units"}
              {:fx/type :button

               :text "All vehicles"}
              {:fx/type :button

               :text "All Infantry"}]}
   {:fx/type :table-view
    :row-factory {:fx/cell-type :table-row
                  :describe (fn [x]
                              {:style {:-fx-border-color :black}})}
    :columns [{:fx/type :table-column
               :text "Unit Name"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (:full-name x)})}}
              {:fx/type :table-column
               :text "Type"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn[x]
                                          {:text (:type x)})}}
              {:fx/type :table-column
               :text "PV"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:point-value x))})}}
              {:fx/type :table-column
               :text "Size"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:size x))})}}
              {:fx/type :table-column
               :text "Movement"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (cu/print-movement x)})}}
              {:fx/type :table-column
               :text "TMM"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:tmm x))})}}
              {:fx/type :table-column
               :text "Armor"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:armor x))})}}
              {:fx/type :table-column
               :text "Structure"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:structure x))})}}
              ;; {:fx/type :table-column
              ;;  :text "Threshold"
              ;;  :cell-value-factory identity
              ;;  :cell-factory {:fx/cell-type :table-cell
              ;;                 :describe (fn [x]
              ;;                             {:text (pr-str (:threshold x))})}}
              {:fx/type :table-column
               :text "S"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (cu/print-short x)})}}
              {:fx/type :table-column
               :text "M"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (cu/print-medium x)})}}
              {:fx/type :table-column
               :text "L"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (cu/print-long x)})}}
              {:fx/type :table-column
               :text "E"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (cu/print-extreme x)})}}
              {:fx/type :table-column
               :text "OV"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (pr-str (:overheat x))})}}
              {:fx/type :table-column
               :text "Abilities"
               :cell-value-factory identity
               :cell-factory {:fx/cell-type :table-cell
                              :describe (fn [x]
                                          {:text (:abilities x)})}}]
    :items (cu/filter-membership (:mul @*state) :type cu/bm-units)}])

(defn force-creation-display []
  [{:fx/type :label :text "Forces"}
   {:fx/type :h-box
    :spacing 5
    :children [{:fx/type :label :text "Force Name:"}
               {:fx/type :text-field
                :text "AFFS"}]}])

(defn unit-selection-display []
  [{:fx/type :label
   :text "Unit List Setup"}])

(defn map-selection-display []
  [{:fx/type :label
   :text "Map Setup"}])

(defn root [{:keys [first-name last-name]}]
  {:fx/type :stage
   :showing true
   :scene
   {:fx/type :scene
    :root
    {:fx/type :grid-pane
     :children [{:fx/type :v-box
                 :spacing 5
                 :fill-width true
                 :alignment :top-center
                 :grid-pane/row 0
                 :grid-pane/column 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :children mul-display}
                {:fx/type :v-box
                 :spacing 5
                 :fill-width true
                 :alignment :top-center
                 :grid-pane/row 0
                 :grid-pane/column 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :children (force-creation-display)}
                {:fx/type :v-box
                 :spacing 5
                 :fill-width true
                 :alignment :top-center
                 :grid-pane/row 1
                 :grid-pane/column 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :children (unit-selection-display)}
                {:fx/type :v-box
                 :spacing 5
                 :fill-width true
                 :alignment :top-center
                 :grid-pane/row 1
                 :grid-pane/column 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :children (map-selection-display)}]}}})

(defn map-event-handler [event]
  (swap! *state assoc (:key event) (:fx/event event)))

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc root)
    :opts {:fx.opt/map-event-handler map-event-handler}))


(defn launch-gui
  []
  (fx/mount-renderer *state renderer))
