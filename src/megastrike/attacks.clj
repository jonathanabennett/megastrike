(ns megastrike.attacks
  "Handles all attack data. Calculates damage numbers, prints damage brackets, calculates to-hit rolls, and produces
  attack options for the GUI to consume."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [megastrike.abilities :as abilities]
   [megastrike.movement :as movement]
   [megastrike.utils :as utils]))

(defn ->ranged-attack
  [{:keys [atk-type s s* m m* l l* e e*] :or {atk-type :regular s 0 s* false m 0 m* false l 0 l* false e 0 e* false}}]
  (let [s (if (= (type s) java.lang.String) (Integer/parseInt s) s)
        s* (if (= (type s*) java.lang.String) (= "True" s*) s*)
        m (if (= (type m) java.lang.String) (Integer/parseInt m) m)
        m* (if (= (type m*) java.lang.String) (= "True" m*) m*)
        l (if (= (type l) java.lang.String) (Integer/parseInt l) l)
        l* (if (= (type l*) java.lang.String) (= "True" l*) l*)
        e (if (= (type e) java.lang.String) (Integer/parseInt e) e)
        e* (if (= (type e*) java.lang.String) (= "True" e*) e*)]
    {atk-type {:attack/s s :attack/s* s* :attack/m m :attack/m* m* :attack/l l :attack/l* l*
               :attack/e e :attack/e* e*}}))

(defn ->physical-attack
  [{:keys [atk-type damage self]}]
  {atk-type {:attack/damage damage :attack/self self}})

(defn add-special-attacks
  [abilities]
  (loop [attacks {}
         atk-abilities (filter #(s/valid? :attack/type (first %)) abilities)]
    (if (empty? atk-abilities)
      attacks
      (recur (let [atk (first atk-abilities)]
               (merge attacks (->ranged-attack (merge (second atk) {:atk-type (first atk)}))))
             (rest atk-abilities)))))

(defn calc-self-charge
  [unit target-size]
  (int (+ (Math/floor (movement/modified-tmm unit)) (if (>= target-size 3) 1 0))))

(defn calc-self-dfa
  [u success?]
  (if success?
    (:unit/size u)
    (inc (:unit/size u))))

(defn ->attacks
  [{:keys [size s s* m m* l l* e e*]} mv-modes abilities]
  (let [size (Integer/parseInt size)]
    (cond-> {}
      true (merge (->ranged-attack {:atk-type :attack/regular
                                    :s s :s* s*
                                    :m m :m* m*
                                    :l l :l* l*
                                    :e e :e* e*}))
      true (merge (->physical-attack
                   {:atk-type :attack/physical
                    :damage (if (abilities/has? abilities :mel)
                              (inc size)
                              size)
                    :self false}))
      true (merge (->physical-attack
                   {:atk-type :attack/charge
                    :damage 0
                    :self true}))
      true (merge (add-special-attacks abilities))
      (movement/has-mode? mv-modes :jump) (merge (->physical-attack
                                                  {:atk-type :attack/dfa
                                                   :damage 0
                                                   :self true})))))

(defn get-size
  [attacks]
  (:size attacks))

(defn get-attack
  [attacks attack]
  (get-in attacks [:attacks attack]))

(defn get-attacks
  [attacks]
  (:attacks attacks))

(defn take-fc-hit
  [attacks]
  (assoc attacks :fc-mod (+ (:fc-mod attacks) 2)))

(defn fc-hits
  [{:keys [fc-mod]}]
  fc-mod)

(defn print-damage-bracket
  [attack bracket]
  (let [bracket* (keyword (str (name bracket) "*"))]
    (if (get attack bracket*)
      "0*"
      (str (get attack bracket)))))

(defn print-damage
  [attacks attack range]
  (let [atk (get-attack attacks attack)]
    (cond
      (and (= range 1) (or (= attack :physical) (= attack :charge) (= attack :dfa))) (print-damage-bracket atk :s)
      (<= range 3) (print-damage-bracket atk :s)
      (<= range 12) (print-damage-bracket atk :m)
      (<= range 21) (print-damage-bracket atk :l)
      (<= range 30) (print-damage-bracket atk :e)
      :else 0)))

(defn weaps-hit-helper
  [kind data]
  (if (some #{kind} [:regular :ht :srm :lrm :if])
    [kind (assoc data
                 :s (max (dec (:s data)) 0)
                 :s* false
                 :m (max (dec (:m data)) 0)
                 :m* false
                 :l (max (dec (:l data)) 0)
                 :l* false
                 :e (max (dec (:e data)) 0)
                 :e* false)]
    [kind data]))

(defn take-weaps-hit
  [attacks]
  (into {} (for [[k v] attacks] (weaps-hit-helper k v))))

(defn calc-charge-damage
  [{:keys [size]} tmm]
  (int (Math/floor (+ size (double (/ tmm 2))))))

(defn calc-dfa-damage
  [attacks tmm]
  (inc (calc-charge-damage attacks tmm)))

(defn roll-damage
  ([attacks attack range rear-attack?]
   (let [damage-str (print-damage attacks attack range)
         damage (if (and (str/ends-with? damage-str "*") (<= 4 (utils/roll-die)))
                  1
                  (Integer/parseInt damage-str))]
     (if rear-attack?
       (inc damage)
       damage)))
  ([attacks attack tmm target-size rear-attack?]
   (let [dmg (condp = attack
               :dfa (calc-dfa-damage attacks tmm)
               :charge (calc-charge-damage attacks tmm)
               :self-charge (calc-self-charge tmm target-size)
               :self-dfa (calc-self-dfa attacks attack)
               :missed-dfa (calc-self-dfa attacks attack))]
     (if rear-attack?
       (inc dmg)
       dmg))))
