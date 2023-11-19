(ns megastrike.gui.core
  (:require [cljfx.api :as fx]))


(defn text-input [{:keys [label]}]
  {:fx/type :v-box
   :children [{:fx/type :label :text label}
              {:fx/type :text-field}]})

(def *state
  (atom {:first-name "Vlad"
         :last-name "Protsenko"}))

(defn text-input [{:keys [label value key]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text label}
              {:fx/type :text-field
               :on-text-changed {:key key}
               :text value}]})

(defn root [{:keys [first-name last-name]}]
  {:fx/type :stage
   :showing true
   :scene
   {:fx/type :scene
    :root
    {:fx/type :grid-pane
     :children [{:fx/type :label
                 :grid-pane/column 0
                 :grid-pane/row 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :text "MUL Setup"}
                {:fx/type :v-box
                 :spacing 5
                 :fill-width true
                 :alignment :top-center
                 :grid-pane/column 1
                 :grid-pane/row 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :children [{:fx/type :label :text "Forces"}
                            {:fx/type :h-box
                             :spacing 5
                             :children [{:fx/type :label :text "Force Name:"}
                                        {:fx/type :text-field
                                         :text "AFFS"}]}]}
                {:fx/type :label
                 :grid-pane/column 0
                 :grid-pane/row 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :text "Unit List Setup"}
                {:fx/type :label
                 :grid-pane/column 1
                 :grid-pane/row 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :text "Map Setup"}]}}})

(defn map-event-handler [event]
  (swap! *state assoc (:key event) (:fx/event event)))

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(fx/mount-renderer *state renderer)
