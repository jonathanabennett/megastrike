(ns megastrike.gui.common
  (:require [cljfx.api :as fx]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]))

(defn prop-label
  [{:keys [label value]}]
  {:fx/type :text-flow
   :children [{:fx/type :text
               :style "-fx-font-weight: bold;"
               :text label}
              {:fx/type :text
               :style "-fx-font-size: 14"
               :text value}]})

(defn draw-sprite
  [{:keys [unit force x y shift]}]
  {:fx/type :image-view
   :image (cu/find-sprite unit)
   :effect {:fx/type :blend
            :top-input {:fx/type :color-input
                        :paint (force :color)
                        :x 0 :y 0 :width 100 :height 100}
            :bottom-input {:fx/type :image-input
                           :source (cu/find-sprite unit)}
            :mode :src-atop
            :opacity 0.5}
   :translate-x x
   :translate-y (+ y shift)
   :x 0
   :y 0})

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

(defn attack-table
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