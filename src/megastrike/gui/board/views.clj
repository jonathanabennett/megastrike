(ns megastrike.gui.board.views
  (:require [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.gui.board.events :as board-events]
            [megastrike.gui.common :as common]
            [megastrike.gui.events :as events]
            [megastrike.gui.subs :as subs]
            [megastrike.hexagons.hex :as hex]))

(defn draw-hex [{:keys [hex layout]}]
  (let [points (hex/hex-points hex layout)
        offset (hex/offset-from-hex hex)
        elevation (:elevation hex)
        terrain (:terrain hex)
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
                 :text (format "%02d%02d" (:x offset) (:y offset))
                 :layout-x (nth points 8)
                 :layout-y (nth points 9)
                 :font 16
                 :translate-x (* 10 (:scale layout))
                 :translate-y (* 10 (:scale layout))}]}))

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
                 :translate-y (* (/ (* (layout :y-size) (:scale layout)) 3) -2)}]}))

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

(defn draw-movement-cost [{:keys [origin destination layout cost]}]
  (let [origin-pixel (hex/hex-to-pixel origin layout)
        dest-pixel (hex/hex-to-pixel destination layout)]
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
        origin (hex/find-hex unit (board/nodes board))
        costs (cu/move-costs unit board)]
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
        active-force (first (subs/turn-order context))
        unit-locations (subs/deployed-units context)
        destinations (filter #(seq (:path %)) (vals (subs/units context)))
        target-lines (filter #(and (= active-force (:force %)) (:target %)) unit-locations)] 
    {:fx/type :scroll-pane 
     :on-key-pressed {:event-type ::events/key-dispatcher :fx/sync true}
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
                              :layout layout}))
                          (when (= (subs/phase context) :combat)
                            (for [t target-lines] 
                              {:fx/type draw-target-line 
                               :unit t 
                               :layout layout}))
                          )}}))