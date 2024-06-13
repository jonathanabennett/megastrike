(ns megastrike.gui.board.views
  (:require [cljfx.api :as fx]
            [megastrike.gui.board.events :as board-events]
            [megastrike.gui.common :as common]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex]
            [megastrike.combat-unit :as cu]))

(defn draw-hex [{:keys [hex layout]}]
  (let [points (hex/hex-points hex layout)
        offset (hex/offset-from-hex hex)]
    {:fx/type :group
     :on-mouse-clicked {:event-type ::board-events/hex-clicked :hex hex}
     :children [{:fx/type :polygon
                 :points points
                 :fill :green
                 :stroke :black}
                {:fx/type :label
                 :text (format "%02d%02d" (:x offset) (:y offset))
                 :layout-x (nth points 4)
                 :layout-y (nth points 5)
                 :font 16
                 :translate-x 10
                 :translate-y -20}]}))

(defn draw-unit [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points unit layout)
        forces (subs/forces context)
        force (forces (unit :force))] 
    {:fx/type :group
     :on-mouse-clicked {:event-type ::events/unit-clicked :unit unit}
     :children [{:fx/type common/draw-sprite 
                 :unit unit 
                 :force force 
                 :x (nth hex 8)
                 :y (nth hex 9)
                 :shift (/ (layout :y-size) 3)}
                {:fx/type :label
                 :text (unit :full-name)
                 :layout-x (nth hex 8)
                 :layout-y (nth hex 9)
                 :font 16
                 :translate-y (/ (layout :y-size) 3)}
                {:fx/type :label 
                 :text (if (:movement-mode unit)
                         (name (:movement-mode unit))
                         "Did not move")
                 :layout-x (nth hex 4)
                 :layout-y (nth hex 5)
                 :font 16
                 :translate-y (* (/ (layout :y-size) 3) -2)}]}))

(defn draw-target-line [{:keys [fx/context unit layout]}]
  (let [origin-hex (hex/hex-to-pixel unit layout)
        target (get (subs/units context) (:target unit))
        target-hex (hex/hex-to-pixel target layout)
        range (hex/hex-distance unit target)
        to-hit (cu/calculate-to-hit unit target)] 
    {:fx/type :group
     :children [{:fx/type :line 
                 :start-x (:x origin-hex)
                 :start-y (:y origin-hex)
                 :end-x (:x target-hex)
                 :end-y (:y target-hex)}
                {:fx/type :label
                 :text (str "Range: " range "; " to-hit "+ To Hit")
                 :layout-x (/ (+ (:x origin-hex) (:x target-hex)) 2)
                 :layout-y (/ (+ (:x origin-hex) (:x target-hex)) 2)
                 :font 16}]}))

(defn draw-destination-token [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points unit layout)
        forces (subs/forces context)
        force (forces (unit :force))] 
    {:fx/type :group 
     :children [{:fx/type common/draw-sprite 
                 :unit unit
                 :force force 
                 :x (nth hex 8)
                 :y (nth hex 9)
                 :shift (/ (layout :y-size) 3)}
                {:fx/type :label 
                 :text "Destination"
                 :layout-x (nth hex 8)
                 :layout-y (nth hex 9)
                 :font 16
                 :translate-y (/ (layout :y-size) 3)}]}))

(defn game-board [{:keys [fx/context]}]
  (let [gb (subs/board context)
        layout (subs/layout context)
        active-force (first (subs/turn-order context))
        unit-locations (subs/deployed-units context)
        destinations (subs/unit-ghosts context)
        target-lines (filter #(and (= active-force (:force %)) (:target %)) unit-locations)] 
    {:fx/type :scroll-pane
     :content {:fx/type :group
               :children (concat
                          (for [h gb]
                            {:fx/type draw-hex
                             :hex h
                             :layout layout})
                          (for [t unit-locations]
                            {:fx/type draw-unit
                             :unit t
                             :layout layout})
                          (for [t destinations]
                            {:fx/type draw-destination-token 
                             :unit t
                             :layout layout})
                          (when (= (subs/phase context) :combat)
                            (for [t target-lines] 
                              {:fx/type draw-target-line 
                               :unit t 
                               :layout layout})))}}))