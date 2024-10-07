(ns megastrike.gui.board.views
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.movement :as movement]
            [megastrike.gui.board.events :as board-events]
            [megastrike.gui.common :as common]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex]
            [megastrike.attacks :as attacks])
  (:import [javafx.scene.control Dialog DialogEvent]))

(defn draw-hex [{:keys [hex layout]}]
  (let [points (hex/points hex layout)
        offset (hex/hex->offset hex)
        {:keys [elevation terrain]} hex
        ;; Colors below come from data/images/hexes/defaultminimap.txt
        sprite (cond
                 (str/includes? terrain "woods") "rgb(180, 230, 130)"
                 (str/includes? terrain "sinkhole") "rgb(210, 180, 150)"
                 (str/includes? terrain "rough") "rgb(186, 191, 160)"
                 (str/includes? terrain "rubble") "rgb(200, 200, 200)"
                 (str/includes? terrain "water") "rgb(200, 247, 253)"
                 (str/includes? terrain "pavement") "rgb(204, 204, 204)"
                 (str/includes? terrain "road") "rgb(71, 79, 107)"
                 (str/includes? terrain "swamp") "rgb(49, 136, 74)"
                 (str/includes? terrain "building") "rgb(204, 204, 204)"
                 (str/includes? terrain "bridge") "rgb(109, 55, 25)"
                 :else "rgb(215, 211, 156)")]
    {:fx/type :group
     :on-mouse-clicked {:event-type ::board-events/hex-clicked :hex hex}
     :children [{:fx/type :polygon
                 :points points
                 :fill sprite
                 :stroke :black}
                {:fx/type :label
                 :text (format "Lvl %s %s" elevation (first (str/split terrain #":")))
                 :layout-x (nth points 4)
                 :layout-y (nth points 5)
                 :font 16
                 :translate-x (* 10 (:scale layout))
                 :translate-y (* -20 (:scale layout))}
                {:fx/type :label
                 :text "Corner 2"
                 :layout-x (nth points 2)
                 :layout-y (nth points 3)
                 :font 16}
                {:fx/type :label
                 :text "Corner 1"
                 :layout-x (nth points 0)
                 :layout-y (nth points 1)
                 :font 16}
                {:fx/type :label
                 :text (format "%02d%02d" (:x offset) (:y offset))
                 :layout-x (nth points 8)
                 :layout-y (nth points 9)
                 :font 16
                 :translate-x (* 10 (:scale layout))
                 :translate-y (* 10 (:scale layout))}]}))

(defn draw-indicator-bar [{:keys [fx/context unit layout]}])

(defn attack-dialog [{:keys [fx/context unit]}]
  {:fx/type :choice-dialog
   :showing (fx/sub-val context get-in [:internal (:id unit) :showing] false)
   :on-close-request (fn [^DialogEvent event]
                       (when (nil? (.getResult ^Dialog (.getSource event)))
                         (.consume event)))
   :header-text "Select Attack"
   :on-hidden {:event-type ::events/close-attack-selection
               :unit unit
               :on-close {:event-type ::events/make-attack
                          :unit unit}}
   :items (fx/sub-val context get-in [:internal (:id unit) :items] [])})

(defn draw-unit [{:keys [fx/context unit layout]}]
  (let [hex (hex/points unit layout)
        forces (subs/forces context)
        force (forces (unit :force))]
    {:fx/type fx/ext-let-refs
     :refs {::dialog {:fx/type attack-dialog
                      :unit unit}}
     :desc {:fx/type :group
            :on-mouse-clicked {:event-type ::events/unit-clicked :unit unit}
            :children [{:fx/type common/draw-sprite
                        :unit unit
                        :force force
                        :x (nth hex 8)
                        :y (nth hex 9)
                        :direction true
                        :shift (/ (* (layout :y-size) (:scale layout)) 3)}
                       {:fx/type :label
                        :text (unit :full-name)
                        :layout-x (nth hex 8)
                        :layout-y (nth hex 9)
                        :font 16
                        :translate-y (/ (* (layout :y-size) (:scale layout)) 3)}
                       {:fx/type :label
                        :text (if (:movement-mode unit)
                                (name (:movement-mode unit))
                                "Did not move")
                        :layout-x (nth hex 4)
                        :layout-y (nth hex 5)
                        :font 16
                        :translate-y (* (/ (* (layout :y-size) (:scale layout)) 3) -2)}]}}))

(defn draw-movement-cost [{:keys [origin destination layout cost]}]
  (let [origin-pixel (hex/hex->pixel origin layout)
        dest-pixel (hex/hex->pixel destination layout)]
    {:fx/type :group
     :children [{:fx/type :line
                 :start-x (:x origin-pixel)
                 :start-y (:y origin-pixel)
                 :end-x (:x dest-pixel)
                 :end-y (:y dest-pixel)}
                {:fx/type :label
                 :text (str cost)
                 :layout-x (:x dest-pixel)
                 :layout-y (:y dest-pixel)
                 :font 16}]}))

(defn draw-movement-path
  [{:keys [fx/context unit layout]}]
  (let [board (subs/board context)
        origin (board/find-hex unit board)
        costs (movement/move-costs unit board)]
    {:fx/type :group
     :children (loop [sprites []
                      total 0
                      costs costs
                      o origin
                      path (:path unit)]
                 (if (empty? path)
                   sprites
                   (recur (concat sprites
                                  [{:fx/type draw-movement-cost
                                    :origin o
                                    :destination (first path)
                                    :layout layout
                                    :cost (+ total (first costs))}])
                          (+ total (first costs))
                          (rest costs)
                          (first path)
                          (rest path))))}))

(defn game-board [{:keys [fx/context]}]
  (let [gb (board/nodes (subs/board context))
        layout (subs/layout context)
        unit-locations (subs/deployed-units context)
        destinations (filter #(seq (:path %)) (vals (subs/units context)))]
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
                          (when (seq destinations)
                            (for [t destinations]
                              {:fx/type draw-movement-path
                               :unit t
                               :layout layout})))}}))
