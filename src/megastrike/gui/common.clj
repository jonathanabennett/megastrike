(ns megastrike.gui.common
  (:require
   [cljfx.api :as fx]
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
  [{:keys [unit force x y]}]
  {:fx/type :image-view
   :image (cu/find-sprite unit)
   :effect {:fx/type :blend
            :top-input {:fx/type :color-input
                        :paint (:color force)
                        :x 0 :y 0 :width 100 :height 100}
            :bottom-input {:fx/type :image-input
                           :source (cu/find-sprite unit)}
            :mode :src-atop
            :opacity 0.5}
   :x x
   :y y})

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
