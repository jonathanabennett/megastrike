(ns megastrike.core
  (:require
   [cljfx.api :as fx]
   [clojure.core.cache :as cache]
   [clojure.string :as str]
   [megastrike.combat-unit :as cu]))

(def *state
  (atom {:mul (cu/filter-membership cu/mul :type cu/ground-units)
         :forces-name "AFFS"
         :forces-color :gold
         :forces-zone "N"
         :forces {}
         :units {}
         :active-unit nil
         :active-force nil
         :game-board []
         :current-phase -1
         :turn-number 0}))

(def empty-game
  (atom {:mul (cu/filter-membership cu/mul :type cu/ground-units)
         :forces-name "AFFS"
         :forces-color :gold
         :forces-zone "N"
         :forces {}
         :units {}
         :active-unit nil
         :active-force nil
         :game-board []
         :current-phase -1
         :turn-number 0}))

(defn new-game
  []
  (reset! *state empty-game))

(defn get-forces
  []
  (:forces @*state))

(defn add-force!
  [{name :name :as force}]
  (let [forces (get-forces)]
    (swap! *state #(assoc % :forces (assoc forces name force)))))

(defn del-force!
  [name]
  (let [forces (get-forces)]
    (swap! *state #(assoc % :forces (dissoc forces name)))))

(defn get-force
  [name]
  (get (get-forces) name))

(defn get-units
  []
  (:units @*state))

(defn add-unit!
  [unit]
  (let [units (get-units)
        full-name (:full-name unit)
        counter (count (remove #(not (str/includes? (first %) full-name)) units))
        id (if (= counter 0)
             (:full-name unit)
             (str full-name " #" (inc counter)))]
    (swap! *state #(assoc % :units (assoc units id unit)))))

(defn del-unit!
  [id]
  (let [units (get-units)]
    (swap! *state #(assoc % :units (dissoc units id)))))

(defn get-unit
  [id]
  (get (get-units) id))

(defn filter-button
  [{:keys [field values text]}]
  {:fx/type :button
   :text text
   :on-action (fn [_] (swap! *state assoc :mul (cu/filter-membership cu/mul field values)))})

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

(defn mul-table [{:keys [mul]}]
   {:fx/type :table-view
    :row-factory {:fx/cell-type :table-row
                  :describe (fn [_]
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
    :items mul})

(defn text-input
  [{:keys [label key value]}]
  {:fx/type :h-box
   :spacing 5
   :children [{:fx/type :label :text label}
              {:fx/type :text-field
               :on-text-changed #(swap! *state assoc key %)
               :text value}]})

(defn forces-table
  [{:keys [forces]}]
  (if (> (count forces) 0)
    {:fx/type :table-view
     :row-factory {:fx/cell-type :table-row
                   :describe (fn [x]
                               {:style {:-fx-border-color (or (:color x) :black)}})}
     :columns [{:fx/type :table-column
                :text "Name"
                :cell-value-factory identity
                :cell-factory {:fx/cell-type :table-cell
                               :describe (fn [x]
                                           {:text (:name x)})}}
                {:fx/type :table-column
                 :text "Deployment"
                 :cell-value-factory identity
                 :cell-factory {:fx/cell-type :table-cell
                                :describe (fn [x]
                                            {:text (:deploy x)})}}
                {:fx/type :table-column
                 :text "Unit Count"
                 :cell-value-factory identity
                 :cell-factory {:fx/cell-type :table-cell
                                :describe (fn [x]
                                            {:text (prn-str (count (filter #(= (:name x) (:force %)) (get-units))))})}}]
     :items (vals forces)}
    {:fx/type :label
     :text "Add a force"}))

(defn unit-selection-display []
  [{:fx/type :label
   :text "Unit List Setup"}])

(defn map-selection-display []
  [{:fx/type :label
   :text "Map Setup"}])

(defn root [{:keys [mul forces]}]
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
                 :children [{:fx/type :label
                             :text "Master Unit List"}
                            mul-filter-buttons
                            {:fx/type mul-table
                             :mul mul}]}
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
                             :key :forces-name
                             :value "AFFS"}
                            {:fx/type text-input
                             :label "Force Deployment"
                             :key :force-zone
                             :value "N"}
                            {:fx/type :h-box
                             :spacing 5
                             :children [{:fx/type :label :text "Color:"}
                                        {:fx/type :color-picker
                                         :on-value-changed #(swap! *state assoc :force-color %)
                                         :value :gold}]}
                            {:fx/type :button
                             :text "Add Force"
                             :on-action (add-force! {:name (:force-name @*state)
                                                          :deployment (:force-zone @*state)
                                                          :color (:force-color @*state)})}

                            {:fx/type forces-table
                             :forces forces}]}
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

(defn -main
  "I don't do a whole lot."
  []
  (fx/mount-renderer *state renderer))
