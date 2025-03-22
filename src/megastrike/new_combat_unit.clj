(ns megastrike.new-combat-unit
  (:require
   [clojure.math :as math]
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.attacks :as attacks]
   [megastrike.board :as board]
   [megastrike.hexagons.hex :as hex]
   [megastrike.movement :as movement]))

(defprotocol Element
  (id [this])
  (set-id [this new-id])
  (full-name [this])
  (chassis [this])
  (model [this])
  (mul-id [this])
  (base-pv [this])
  (element-type [this])
  (role [this])
  (threshold [this])
  (battle-force [this])
  (set-battle-force [this new-force])
  (pilot [this])
  (set-pilot [this new-pilot])
  (acted? [this])
  (take-action [this])
  (end-phase [this]))

(defrecord CombatUnit [id full-name chassis model mul-id threshold
                       base-pv role element-type
                       battle-force pilot acted? abilities
                       ;; Movement variables
                       modes tmm mv-hits selected default location path facing
                       ;; Attacks variables
                       size fc-hits attacks
                       ;; Heat Variables
                       current-heat overheat overheat-used]
  Element
  (id [_] id)
  (set-id [this new-id] (assoc this :id new-id))
  (full-name [_] full-name)
  (chassis [_] chassis)
  (model [_] model)
  (mul-id [_] mul-id)
  (base-pv [_] base-pv)
  (element-type [_] element-type)
  (role [_] role)
  (threshold [_] threshold)
  (battle-force [_] battle-force)
  (set-battle-force [this new-force] (assoc this :battle-force new-force))
  (pilot [_] pilot)
  (set-pilot [this new-pilot] (assoc this :pilot new-pilot))
  (acted? [_] acted?)
  (take-action [this] (assoc this :acted? true))
  (end-phase [this] (assoc this :acted? false))

  movement/Moveable
  (movement/get-modes [_] modes)
  (movement/has-mode? [_ mode] (contains? modes mode))
  (movement/get-default [_] default)
  (movement/get-selected
    ([_] selected)
    ([_ accept-default?]
     (if (and (not selected) accept-default?) default selected)))
  (movement/set-selected [this new-mode] (assoc this :selected new-mode))
  (movement/select-default [this] (if (not selected) (movement/set-selected this default) this))
  (movement/clear-selected [this] (assoc this :selected nil))
  (movement/get-location [_] location)
  (movement/set-location [this new-location]
    (assoc this :location (select-keys new-location [:p :q :r])))
  (movement/deployed? [_] (get location :q false))
  (movement/get-facing [_] (get movement/directions facing))
  (movement/get-rear [this] (:rear (movement/get-facing this)))
  (movement/set-facing [this new-facing] (assoc this :facing new-facing))

  (movement/mv-hits [_] mv-hits)
  (movement/immobilize [this] (assoc this :modes {:immobilized 0} :default :immobilized))
  (movement/take-mv-hit [this]
    (if (< mv-hits 3)
      (assoc this :mv-hits (inc mv-hits))
      (movement/immobilize this)))

  (movement/get-mv [this move-type]
    (let [base-move (move-type (movement/get-modes this))]
      (loop [mv base-move
             n 0]
        (if (= n mv-hits)
          (max (- mv current-heat) 0)
          (recur (let [new-mv (math/round (/ mv 2.0))]
                   (if (>= (- mv new-mv) 1) new-mv 0))
                 (inc n))))))
  (movement/get-mv [this] (movement/get-mv this selected))
  (movement/get-tmm [_]
    (let [tmm (loop [value tmm
                     n 0]
                (if (= n mv-hits)
                  value
                  (recur (let [new-tmm (math/round (/ tmm 2.0))]
                           (if (>= (- value new-tmm) 1) new-tmm 0))
                         (inc n))))]
      (if (> current-heat 2)
        (dec tmm)
        tmm)))
  (movement/get-tmm-data [this]
    (let [jump-mod (:value (or (abilities/has? abilities :jmpw) (abilities/has? abilities :jmps) {:value 0}))
          s (movement/get-selected this true)]
      (condp = s
        :immobile -4
        :stand-still 0
        :jump (+ jump-mod (movement/get-tmm this) 1)
        (movement/get-tmm this))))

  (movement/cancel-movement [this] (assoc this :path [] :selected false))
  (movement/can-move? [this]
    (.can-move? this path))
  (movement/can-move? [this path]
    (cond
      (not (hex/same-hex (first path) (movement/get-location this)))
      (do (mu/log ::move-failed
                  :reason "path doesn't start at the unit's location.") false)
      (get (last path) :stacking false)
      (do (mu/log ::move-failed
                  :reason "Path ends in an occupied hex.") false)

    ;; The path crosses a hex occupied by an enemy unit

      ; (unblocked-path? path unit-force) (do (mu/log ::move-failed
      ;                                               :reason "Path crosses a hex occupied by an enemy unit.") false)

    ;; Units standing still or immobile should have no path
      (and (contains? #{:stand-still :immobile} (movement/get-selected this false)) (empty? path)) true

      (pos? (count path)) (<= (reduce + (board/path-cost path (movement/get-selected this false) battle-force)) (movement/get-mv this))
      :else (do (mu/log ::move-failed
                        :reason "unknown"
                        :unit this
                        :path path)
                false)))

  (movement/get-path [_] path)
  (movement/find-path [this destination board]
    (loop [path (movement/astar (movement/get-location this) destination board hex/distance (movement/get-selected this true) battle-force)]
      (if (or (empty? path) (movement/can-move? this path))
        path
        (recur (butlast path)))))

  (movement/set-path [this destination board]
    (-> this
        (movement/select-default)
        (assoc :path (movement/find-path this destination board))))
  (movement/set-path [this path]
    (-> this
        (movement/select-default)
        (assoc :path path)))
  (movement/move-unit [this]
    (let [u (movement/select-default this)]
      (if (.can-move? u)
        (-> u
            (movement/set-location (last path))
            (assoc :path []))
        this)))

  attacks/MakesAttacks
  (size [_] size)
  (attacks [this])
  (add-attack [this new-attack])
  (get-attack [this atk])
  (fc-damage [this])
  (take-fc-damage) [this]
  (take-weaps-hit [this])
  (generate-firing-solution [this target board layout] [this target board layout attack])
  (declare-special-attack [this firing-solution])
  (make-dfa-attack [this to-hit])
  (make-charge-attack [this to-hit])
  (make-basic-attack [this firing-solution to-hit])
  (make-heat-attack [this firing-solution to-hit]))
