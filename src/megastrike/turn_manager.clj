(ns megastrike.turn-manager
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.combat-unit :as cu]))

(defn deploy-unit
  [{:keys [active-unit units turn-order] :as game-state}]
  (let [unit (get units active-unit)]
    (if (cu/deployed? unit)
      (do (mu/log ::unit-deployed
                  :unit unit)
          (assoc game-state
                 :turn-order (rest turn-order)
                 :units (assoc units active-unit (cu/take-action unit))
                 :active-unit nil
                 :turn-flag false))
      (do (mu/log ::deployment-failed
                  :unit unit)
          (assoc game-state
                 :active-unit nil
                 :turn-flag false)))))

(defn switch-unit
  [{:keys [active-unit units turn-order] :as game-state} new-active]
  (let [old-active (get units active-unit)
        active-id (if (and (= (cu/get-force new-active) (first turn-order)) (not (cu/acted? new-active)))
                    (cu/id new-active)
                    old-active)]
    (assoc game-state
           :active-unit active-id
           :turn-flag false)))

(defn confirm-move
  [{:keys [active-unit units turn-order game-board] :as game-state}]
  (mu/log ::confirming-move)
  (let [unit (get units active-unit)
        moved-unit (if (= (first turn-order) (cu/get-force unit))
                     (cu/move-unit unit game-board)
                     unit)]
    (if (cu/acted? moved-unit)
      (do (mu/log ::move-confirmed
                  :unit moved-unit
                  :destination (cu/get-location moved-unit)
                  :remaining-moves (rest turn-order)
                  :instrumentation :player)
          (assoc game-state
                 :turn-order (rest turn-order)
                 :units (assoc units (cu/id moved-unit) moved-unit)
                 :turn-flag nil
                 :active-unit nil))
      (do (mu/log ::move-failed
                  :origin (cu/get-location moved-unit)
                  :force (cu/get-force moved-unit)
                  :force-conditional (= (first turn-order) (cu/get-force unit))
                  :active (cu/id unit)
                  :path (cu/get-path unit))
          (assoc game-state :turn-flag nil)))))

(defn make-attack
  [{:keys [units round-report] :as game-state} targeting]
  (let [attack-result (cu/make-attack targeting)
        units (merge units (:result attack-result))
        report (str round-report (reports/parse-attack-data attack-result))]
    (assoc game-state :units units :round-report report)))

(defn make-attacks
  [game-state targeting-list]
  (loop [game-state game-state
         targeting-list targeting-list]
    (if (empty? targeting-list)
      game-state
      (recur
       (let [targeting (first targeting-list)]
         (make-attack game-state targeting))
       (rest targeting-list)))))
