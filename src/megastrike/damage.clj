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

(defn ->damage
  [{:keys [armor structure damage crits] :or {crits []}}]
  (let [arm (if (= (type armor) java.lang.String)
              (Integer/parseInt armor)
              (:current armor))
        struct-int (if (= (type structure) java.lang.String)
                     (Integer/parseInt structure)
                     (:current structure))
        damage (if damage
                 damage
                 {:armor arm
                  :structure struct-int
                  :crits []})]
    {:armor (if (= (type armor) java.lang.String)
              {:current arm
               :maximum arm}
              armor)
     :structure (if (= (type structure) java.lang.String)
                  {:current struct-int
                   :maximum struct-int}
                  structure)
     :changes damage
     :crits crits}))

(defn get-max
  [combat kind]
  (get-in combat [kind :maximum]))

(defn get-current
  [combat kind]
  (get-in combat [kind :current]))

(defn get-remaining-armor
  [{:keys [changes]}]
  (:armor changes))

(defn get-remaining-structure
  [{:keys [changes]}]
  (:structure changes))

(defn get-new-crits
  [{:keys [changes]}]
  (:crits changes))

(defn get-crits
  [{:keys [crits] :as combat}]
  (concat crits (get-new-crits combat)))

(defn get-heat
  [{:keys [changes]}]
  (get changes :heat 0))

(defn destroyed?
  [combat]
  (or (:destroyed? combat) (not (pos? (get-current combat :structure)))))

(defn apply-damage
  [damage]
  (let [armor (get-remaining-armor damage)
        structure (get-remaining-structure damage)
        crits (get-crits damage)
        destroyed? (get-in damage [:changes :destroyed?] false)]
    (-> damage
        (assoc-in [:armor :current] armor)
        (assoc-in [:structure :current] structure)
        (assoc :crits crits)
        (assoc :destroyed? destroyed?)
        (assoc :changes {:armor armor
                         :structure structure
                         :crits []
                         :destroyed? destroyed?}))))

(defn roll-crits
  [tac penetration]
  (let [tac-crit (if tac (get criticals (utils/roll2d) nil) nil)
        pen-crit (if (pos? penetration) (get criticals (utils/roll2d) nil) nil)]
    (mu/log ::roll-crits
            :tac-crit tac-crit
            :penetration penetration
            :pen-crit pen-crit)
    [tac-crit pen-crit]))

(declare take-damage)

(defn take-crits
  [damage abilities crits]
  (loop [damage damage
         crits crits]
    (if (empty? crits)
      damage
      (recur
       (let [crit (first crits)
             changes (:changes damage)]
         (case crit
           :ammo (let [case-ability (abilities/has? abilities :case)
                       case2 (abilities/has? abilities :caseii)
                       ene (abilities/has? abilities :ene)]
                   (cond (or case2 ene) damage
                         case-ability (take-damage damage abilities 1)
                         :else (assoc damage :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :ammo)))))
           :engine (if (some #(= % :engine) (get-crits damage))
                     (assoc damage :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :engine)))
                     (assoc-in damage [:changes :crits] (conj (get-in damage [:changes :crits]) :engine)))
           :fire-control (if (< (count (filter #(= % :fire-control) (get-crits damage))) 4)
                           (assoc-in damage [:changes :crits] (conj (get-in damage [:changes :crits]) :fire-control))
                           damage)
           :weapon (assoc-in damage [:changes :crits] (conj (get-in damage [:changes :crits]) :weapon))
           :mv (assoc-in damage [:changes :crits] (conj (get-in damage [:changes :crits]) :mv))
           :destroyed (assoc damage :changes (assoc changes :destroyed? true :crits (conj (:crits changes) :destroyed)))
           damage))
       (rest crits)))))

(defn heat-damage
  [{:keys [damage] :as target} damage-num]
  (let [ext-heat (get-in damage [:changes :external-heat] 0)]
    (assoc-in target [:damage :changes :external-heat] (min (+ ext-heat damage-num) 2))))

(defn take-damage
  ([target damage]
   (take-damage target damage false))
  ([{:keys [damage abilities] :as target} damage-num tac]
   (if (zero? damage-num)
     {:crit [nil nil] :result target}
     (let [armor (max (- (get-remaining-armor damage) damage-num) 0)
           penetration (- damage-num (get-remaining-armor damage))
           structure (if (zero? armor)
                       (- (get-remaining-structure damage) penetration)
                       (get-remaining-structure damage))
           crits (roll-crits tac penetration)
           upd (if crits
                 (take-crits (assoc damage :changes {:armor armor :structure structure}) abilities crits)
                 (assoc damage :changes {:armor armor :structure structure}))]
       (assoc target :damage upd)))))

