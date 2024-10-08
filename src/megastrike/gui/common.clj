(ns megastrike.gui.common
  (:require [cljfx.api :as fx]
            [megastrike.board]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events])
  (:import [javafx.scene.control Dialog DialogEvent]))

(defn attack-dialog
  [{:keys [fx/context]}]
  (let [unit (fx/sub-val context get-in [:internal :attack-dialog :unit])]
    {:fx/type :choice-dialog
     :showing (fx/sub-val context get-in [:internal :attack-dialog :showing] false)
     :on-close-request (fn [^DialogEvent event]
                         (when (nil? (.getResult ^Dialog (.getSource event)))
                           (.consume event)))
     :header-text "Select Attack"
     :on-hidden {:event-type ::events/close-attack-selection
                 :unit unit
                 :on-close {:event-type ::events/make-attack :unit unit}}
     :items (fx/sub-val context get-in [:internal :attack-dialog :items] [])}))

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

(defn draw-sprite
  "Draws a sprite. Used for both the map and the lobby."
  [{:keys [unit force x y shift direction]}]
  (let [{:keys [color camo] :or {color "#FFFFFF"}} force
        img (cu/find-sprite unit)]
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
                             :source (str (.toURI (cu/find-sprite unit)))}
              :mode :src-atop
              :opacity 0.5}
     :rotate (if direction
               (get-in cu/directions [(:direction unit) :angle] 0)
               0)
     :translate-x x
     :translate-y (+ y shift)
     :x 0
     :y 0}))

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
