(ns megastrike.gui.board.views
  (:require [cljfx.api :as fx]
            [megastrike.gui.board.events :as board-events]
            [megastrike.gui.common :as common]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex]))

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
        forces (fx/sub-val context :forces)
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

(defn draw-destination-token [{:keys [fx/context unit layout]}]
  (let [hex (hex/hex-points (:destination unit) layout)
        forces (fx/sub-val context :forces)
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
  (let [gb (fx/sub-val context :game-board)
        layout (fx/sub-val context :layout)
        units (vals (subs/units context))
        unit-locations (loop [i 0
                      unit-locations []]
                 (if (= (count units) i)
                   unit-locations
                   (recur
                    (inc i)
                    (if (:q (nth units i))
                      (conj unit-locations (nth units i))
                      unit-locations))))
        destinations (loop [i 0
                            destinations []]
                       (if (= (count units) i)
                         destinations
                         (recur 
                          (inc i)
                          (if (:destination (nth units i))
                            (conj destinations (nth units i))
                            destinations))))] 
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
                             :layout layout}))}}))

