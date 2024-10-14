(ns megastrike.gui.common
  (:require [cljfx.api :as fx]
            [megastrike.board]
            [megastrike.attacks :as attacks]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.events :as events]
            [com.brunobonacci.mulog :as mu]
            [megastrike.gui.subs :as subs])
  (:import [javafx.scene.control Dialog DialogEvent]))

(defn attack-buttons
  [attacks unit]
  (prn attacks)
  (loop [ret [{:fx/type :button
               :text "No Attack"
               :on-action {:event-type ::events/close-attack-selection :selected false :unit unit}}]
         attacks attacks]
    (if (empty? attacks)
      ret
      (recur (let [atk-data (first (vals (first attacks)))]
               ((comp vec flatten conj) ret {:fx/type :button
                                             :text (attacks/print-attack-roll atk-data false)
                                             :on-action {:event-type ::events/close-attack-selection  :unit unit :selected atk-data :fx/sync true}}))
             (rest attacks)))))

(defn attack-dialog
  [{:keys [fx/context]}]
  (let [unit (fx/sub-val context get-in [:internal :attack-dialog :unit])
        attacks (fx/sub-val context get-in [:internal :attack-dialog :items])
        active (subs/active-unit context)]
    {:fx/type :dialog
     :showing (fx/sub-val context get-in [:internal :attack-dialog :showing] false)
     :on-close-request (fn [^DialogEvent event]
                         (when (nil? (.getResult ^Dialog (.getSource event)))
                           (.consume event)))
     :header-text (str (:full-name active) " attacking " (:full-name unit))
     :on-hidden {:event-type ::events/close-attack-selection
                 :unit unit
                 :on-close {:event-type ::events/make-attack :unit unit}}
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:cancel]
                   :content {:fx/type :v-box
                             :spacing 5
                             :children (attack-buttons attacks unit)}}}))

(defn round-dialog
  [{:keys [fx/context]}]
  (let [round (subs/turn-number context)
        phase (name (subs/phase context))
        round-report (fx/sub-val context :round-report)]
    (mu/log ::round-report
            :round round
            :phase phase
            :report round-report)
    {:fx/type :dialog
     :showing (fx/sub-val context get-in [:internal :round-dialog :showing] false)
     :on-close-request (fn [^DialogEvent event]
                         (when (nil? (.getResult ^Dialog (.getSource event)))
                           (.consume event)))
     :header-text (str "Turn " round " / " phase " phase")
     :on-hidden {:event-type ::events/hide-popup
                 :state-id :round-dialog}
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:ok]
                   :content {:fx/type :scroll-pane
                             :content {:fx/type :text
                                       :text round-report}}}}))

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
