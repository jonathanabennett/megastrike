(ns megastrike.gui.common
  (:require [cljfx.api :as fx]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]))

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
  [{:keys [unit force x y shift]}]
  (let [color (force :color "#FFFFFF")
        camo (force :camo)
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
