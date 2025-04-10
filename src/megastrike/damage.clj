(ns megastrike.damage
  (:require
   [com.brunobonacci.mulog :as mu]
   [megastrike.abilities :as abilities]
   [megastrike.utils :as utils]))

(def criticals
  {2 :ammo
   3 :engine
   4 :fire-control
   6 :weapon
   7 :mv
   8 :weapon
   10 :fire-control
   11 :engine
   12 :destroyed})

(defn crit-count
  [unit crit-type]
  (or (count (filter #(= crit-type %) (get-in unit [:unit/criticals :crits/taken]))) 0))

(defn remaining-structure
  [unit]
  (- (get-in unit [:unit/structure :toughness/current]) (get-in unit [:unit/structure :toughness/unapplied])))

(defn remaining-armor
  [unit]
  (- (get-in unit [:unit/armor :toughness/current]) (get-in unit [:unit/armor :toughness/unapplied])))

(defn roll-crits
  [penetration tac]
  (let [tac-crit (if tac (get criticals (utils/roll2d) nil) nil)
        pen-crit (if penetration (get criticals (utils/roll2d) nil) nil)]
    (mu/log ::roll-crits
            :tac-crit tac-crit
            :penetration penetration
            :pen-crit pen-crit)
    [tac-crit pen-crit]))

(declare take-damage)

(defn destroyed-by-crit
  [unit crit]
  (-> unit
      (update-in unit [:unit/criticals :crits/taken] conj crit)
      (assoc :unit/destroyed? true)))

(defn ammo-crit
  [unit]
  (let [case-ability (abilities/has? unit :case)
        case2 (abilities/has? unit :caseii)
        ene (abilities/has? unit :ene)]
    (cond (or case2 ene) unit
          case-ability (-> unit
                           (update-in [:unit/criticals :crits/taken] conj :crits/ammo)
                           (take-damage 1))

          :else (destroyed-by-crit unit :crits/ammo))))

(defn engine-crit
  [unit]
  (if (pos? (crit-count unit :crits/engine))
    (destroyed-by-crit unit :crits/engine)
    (update-in unit [:unit/criticals :crits/taken] conj :crits/engine)))

(defn take-crit
  [unit crit]
  (condp = crit
    :crits/ammo (ammo-crit unit)
    :crits/engine (engine-crit unit)
    :crits/fire-control (update-in unit [:unit/criticals :crits/taken] conj :crits/fire-control)
    :crits/weapon (update-in unit [:unit/criticals :crits/taken] conj :crits/weapon)
    :crits/mv (update-in unit [:unit/criticals :crits/taken] conj :crits/mv)
    :crits/destroyed (destroyed-by-crit unit :crits/destroyed)
    unit))

(defn take-crits
  [unit]
  (loop [unit unit
         crits (get-in unit [:unit/criticals :crits/unapplied])]
    (if (empty? crits)
      unit
      (recur
       (do
         (mu/log ::taking-crit
                 :damage unit
                 :crit (first crits))
         (take-crit unit (first crits)))
       (rest crits)))))

(defn apply-damage
  [unit]
  (-> unit
      (update-in [:unit/armor :toughness/current] - (get-in unit [:unit/armor :toughness/unapplied] 0))
      (assoc-in [:unit/armor :toughness/unapplied] 0)
      (update-in [:unit/structure :toughness/current] - (get-in unit [:unit/structure :toughness/unapplied] 0))
      (assoc-in [:unit/structure :toughness/unapplied] 0)
      (take-crits)))

(defn heat-damage
  [unit new-heat]
  (let [unapplied-heat (get unit :unit/unapplied-heat 0)]
    (assoc unit :unit/unapplied-heat (min (+ unapplied-heat new-heat) 2))))

(defn take-damage
  ([unit damage]
   (take-damage unit damage false))
  ([unit damage tac]
   (if (zero? damage)
     unit
     ;; Check how much damage should be applied to armor
     ;; Check how much damage should be applied to structure
     ;; Generate the appropriate number of criticals
     ;; Thread unit through updating the unapplied values.
     (let [armor-damage (min (remaining-armor unit) damage)
           penetration (- damage armor-damage)
           crits (roll-crits (pos? penetration) tac)]
       (-> unit
           (update-in [:unit/armor :toughness/unapplied] + armor-damage)
           (update-in [:unit/structure :toughness/unapplied] + penetration)
           (update-in [:unit/criticals :crits/unapplied] merge crits))))))

