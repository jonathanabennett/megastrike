(ns megastrike.gui.common
  (:require
   [cljfx.api :as fx]
   [megastrike.gui.events :as events]
   [megastrike.gui.subs :as sub]))

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
