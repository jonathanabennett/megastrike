(ns megastrike.attacks
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [megastrike.board :as board]
            [megastrike.hexagons.hex :as hex]))

(def probabilities 
  {2  100, 3  98, 4  92, 5  83, 6  72, 7  58, 8  42, 9  28, 10 17, 11 8, 12 3})

(defn get-tmm
  ([unit]
   (let [div (count (filter #(= :mv %) (:crits unit)))]
     (loop [tmm (get unit :tmm)
            n 0]
       (if (= n div)
         tmm
         (recur (let [new-tmm (math/round (/ tmm 2.0))]
                  (if (>= (- tmm new-tmm) 1) new-tmm 0))
                (inc n)))))))

(defn attacker-skill
  [unit]
  [{:desc "pilot skill" :value (get-in unit [:pilot :skill])}])

(defn calculate-amm
  [unit]
  (condp = (:movement-mode unit)
    :immobile [{:desc "attacker immobile" :value -1}]
    :stand-still [{:desc "attacker stood still" :value -1}]
    :jump [{:desc "attacker jumped" :value 2}]
    [{:desc "attacker moved" :value 0}]))

(defn calculate-fc-hits
  [unit]
  (let [fc (count (filter #(= % :fire-control) (:crits unit)))
        s (if (= fc 1)
            "fire control hit"
            "fire control hits")]
      [{:desc (str fc " " s) :value (* fc 2)}]))

(defn calculate-target-mod
  [unit]
  (condp = (:movement-mode unit)
    :immobile [{:desc "target immobile" :value -4}]
    :stand-still [{:desc "target did not move" :value 0}]
    :jump [{:desc "target jumped" :value (inc (get-tmm unit))}]
    [{:desc "target moved" :value (get-tmm unit)}]))

(defn calculate-heat-mod 
  [unit]
  (let [heat (get unit :current-heat 0)]
    (when (pos? heat)
      [{:desc "attacker heat" :value heat}])))

(defn woods-mod
  [line]
  (let [woods (count (filter #(str/includes? (:terrain %) "woods") (rest line)))]
    (cond
      (>= woods 3) [{:desc "Line of Sight blocked by woods" :value ##Inf}]
      (str/includes? (:terrain (last line)) "woods") [{:desc "target in woods" :value 1}]
      (pos? woods) [{:desc "intervening woods" :value 1}]
      :else [{:desc "no intervening woods" :value 0}])))

(defn calculate-range-mod 
  [range]
  (let [range-str (str "target " range " hexes away")]
    (condp >= range 
      3  [{:desc range-str :value 0}] 
      12 [{:desc range-str :value 2}] 
      21 [{:desc range-str :value 4}] 
      30 [{:desc range-str :value 6}] 
      [{:desc "Target out of range" :value ##Inf}])))

(defn height-checker
  [origin target line]
  (let [o-height (+ 2 (:elevation (first line)))
        t-height (+ 2 (:elevation (last line)))]
    (if (= (count line) 2)
      false
      (loop [blocked? false
            current (first line)
            l (rest line)]
       (if (or blocked? (= (count l) 1))
         blocked?
         (recur (cond
                  (hex/same-hex origin current)   (>= (:elevation current) o-height)
                  (hex/same-hex target (first l)) (>= (:elevation current) t-height)
                  :else (and (>= (:elevation current) o-height) (>= (:elevation current) t-height)))
                (first l)
                (rest l)))))))
            
(defn produce-attack-roll
  [attacker target board]
  (let [line (board/hex-line attacker target board) 
        attack-roll (conj [] (attacker-skill attacker) 
                             (calculate-fc-hits attacker) 
                             (calculate-amm attacker) 
                             (calculate-target-mod target) 
                             (calculate-heat-mod attacker) 
                             (if (height-checker attacker target line) 
                               [{:desc "Line of Sight Blocked" :value ##Inf}]
                               [{:desc "clear line of sight" :value 0}])
                             (woods-mod line) 
                             (calculate-range-mod (hex/hex-distance attacker target)))]
    attack-roll))

(defn calculate-to-hit 
  [attack-roll]
  (reduce + (map #(:value (first %) 0) attack-roll)))

(defn attack-roll-parser 
  [m]
    (let [m (first m)
          s (if (neg? (:value m 0))
              (str "- " (abs (:value m 0)) " (" (:desc m) ") ")
              (str "+ " (:value m 0) " (" (:desc m) ") "))]
      s))

(defn print-attack-roll 
  ([attack-roll]
   (print-attack-roll attack-roll true))
  ([attack-roll detailed?]
   (if (some #(= ##Inf (:value (first %))) attack-roll)
     (:desc (first (first (filter #(= ##Inf (:value (first %))) attack-roll))))
     (let [to-hit (calculate-to-hit attack-roll) 
           to-hit-str (str "To Hit: " to-hit " (" (get probabilities to-hit) "%)")] 
       (if detailed?  
         (let [details (map attack-roll-parser attack-roll)]
           (str/trim (str to-hit-str ": " (reduce str details))))
         (str/trim to-hit-str))))))
