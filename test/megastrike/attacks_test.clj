(ns megastrike.attacks-test
  (:require
   [clojure.test :as t]
   [megastrike.attacks :as sut]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]))

(def board (board/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board"))
(def layout (hex/create-layout))
(def attacker1 (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                 {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                 :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0))
(def target1 (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                               {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                               :direction/n {:hex/p 2 :hex/q 1 :hex/r -3} :1stsomersetstrikers 0))
(def wooded-unit (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                   {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                   :direction/n {:hex/p 4 :hex/q 0 :hex/r -4} :1stsomersetstrikers 0))
(def blinded-attacker (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                        {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                        :direction/n {:hex/p 3 :hex/q 4 :hex/r -7} :1stsomersetstrikers 0))
(def blinded-target (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                      {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                      :direction/n {:hex/p 7 :hex/q 2 :hex/r -9} :1stsomersetstrikers 0))
(def heated-attacker (assoc (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                              {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                              :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0) :unit/current-heat 1))

(t/deftest print-damage
  (t/testing "Printing regular damage"
    (t/is (= (sut/print-damage attacker1 :attack/regular 0) "3"))))

(t/deftest woods-mod-test
  (let [woods-hex {:p 8, :q 6, :r -14, :elevation 0, :terrain "woods:1:20;ground_fluff:1:2;foliage_elev:2", :palette "grass"}
        empty-hex {:p 12, :q 4, :r -16, :elevation 0, :terrain "", :palette "grass"}
        rough-hex {:p 13, :q 4, :r -17, :elevation 0, :terrain "rough:1:20", :palette "grass"}
        ground-fluff-hex {:p 2, :q 9, :r -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}]
    (t/testing "Calculate woods mod"
      (t/is (= (sut/woods-mod [empty-hex rough-hex ground-fluff-hex]) {:targeting/description "No intervening woods" :targeting/value 0}) "Testing no woods at all")
      (t/is (= (sut/woods-mod [woods-hex empty-hex empty-hex]) {:targeting/description "No intervening woods" :targeting/value 0}) "Testing attacker standing in woods.")
      (t/is (= (sut/woods-mod [empty-hex woods-hex empty-hex]) {:targeting/description "Intervening woods" :targeting/value 1}) "Empty, Woods, Empty")
      (t/is (= (sut/woods-mod [empty-hex empty-hex woods-hex]) {:targeting/description "Target in woods" :targeting/value 1}) "Empty, Empty, Woods")
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex]) {:targeting/description "Target in woods" :targeting/value 1}) "Empty, Woods, Woods")
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex empty-hex]) {:targeting/description "Intervening woods" :targeting/value 1}) "Empty, woods, woods, empty")
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex woods-hex woods-hex]) {:targeting/description "Line of Sight blocked by woods" :targeting/value ##Inf}) "LOS Blocked"))))

(t/deftest calculate-distance-mod-test
  (t/testing "Calculate distance mod"
    (t/is (= (sut/calculate-distance-mod 1)  {:targeting/description "Target 1 hexes away"  :targeting/value 0}))
    (t/is (= (sut/calculate-distance-mod 2)  {:targeting/description "Target 2 hexes away"  :targeting/value 0}))
    (t/is (= (sut/calculate-distance-mod 3)  {:targeting/description "Target 3 hexes away"  :targeting/value 0}))
    (t/is (= (sut/calculate-distance-mod 4)  {:targeting/description "Target 4 hexes away"  :targeting/value 2}))
    (t/is (= (sut/calculate-distance-mod 5)  {:targeting/description "Target 5 hexes away"  :targeting/value 2}))
    (t/is (= (sut/calculate-distance-mod 11) {:targeting/description "Target 11 hexes away" :targeting/value 2}))
    (t/is (= (sut/calculate-distance-mod 12) {:targeting/description "Target 12 hexes away" :targeting/value 2}))
    (t/is (= (sut/calculate-distance-mod 13) {:targeting/description "Target 13 hexes away" :targeting/value 4}))
    (t/is (= (sut/calculate-distance-mod 14) {:targeting/description "Target 14 hexes away" :targeting/value 4}))
    (t/is (= (sut/calculate-distance-mod 15) {:targeting/description "Target 15 hexes away" :targeting/value 4}))
    (t/is (= (sut/calculate-distance-mod 20) {:targeting/description "Target 20 hexes away" :targeting/value 4}))
    (t/is (= (sut/calculate-distance-mod 21) {:targeting/description "Target 21 hexes away" :targeting/value 4}))
    (t/is (= (sut/calculate-distance-mod 22) {:targeting/description "Target 22 hexes away" :targeting/value 6}))
    (t/is (= (sut/calculate-distance-mod 29) {:targeting/description "Target 29 hexes away" :targeting/value 6}))
    (t/is (= (sut/calculate-distance-mod 30) {:targeting/description "Target 30 hexes away" :targeting/value 6}))
    (t/is (= (sut/calculate-distance-mod 31) {:targeting/description "Target 31 hexes away" :targeting/value ##Inf}))))

(t/deftest ->targeting-test
  (t/testing "Testing attack rolls"
    (t/is (= (sut/->targeting attacker1 target1 board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Attacker1 -> Target1")
    (t/is (= (sut/->targeting heated-attacker target1 board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 1}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Heated attacker -> target 1")
    (t/is (= (sut/->targeting wooded-unit target1 board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 2 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 2, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Wooded attacker -> target 1")
    (t/is (= (sut/->targeting attacker1 wooded-unit board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 3 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "Target in woods", :targeting/value 1}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 3, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Attacker1 -> Wooded unit")
    (t/is (= (sut/->targeting target1 attacker1 board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 1, :targeting/rear-attack? true, :targeting/target "Wolfhound WLF-2"}}) "Target 1 -> Attacker 1")
    (t/is (= (sut/->targeting blinded-attacker blinded-target board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Line of sight blocked", :targeting/value ##Inf}, :targeting/range-mod {:targeting/description "Target 4 hexes away", :targeting/value 2}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 4, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Blinded attacker -> Blinded target")
    (t/is (= (sut/->targeting blinded-target blinded-attacker board layout :attack/regular)
             {:attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Line of sight blocked", :targeting/value ##Inf}, :targeting/range-mod {:targeting/description "Target 4 hexes away", :targeting/value 2}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 4, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}) "Blinded Target -> Blinded Attacker")))

(t/deftest calculate-to-hit-test
  (t/testing "return to-hit numbers for attacks"
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting attacker1 target1 board layout :attack/regular))) 6) "Attacker1 -> Target1")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting heated-attacker target1 board layout :attack/regular))) 7) "Heated-attacker -> Target1")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting wooded-unit target1 board layout :attack/regular))) 6) "Wooded attacker -> Target1")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting attacker1 wooded-unit board layout :attack/regular))) 7) "Attacker1 -> Wooded target")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting target1 attacker1 board layout :attack/regular))) 6) "Target 1 -> Attacker 1")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting blinded-attacker blinded-target board layout :attack/regular))) ##Inf) "Blinded attacker -> Blinded target")
    (t/is (= (sut/calculate-to-hit (:attack/regular (sut/->targeting blinded-target blinded-attacker board layout :attack/regular))) ##Inf) "Blinded target -> Blinded Attacker")))

(t/deftest print-attack-roll
  (t/testing "Return minimum attack roll result"
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting attacker1 target1 board layout :attack/regular)) false) "To Hit: 6 (72%)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting heated-attacker target1 board layout :attack/regular)) false) "To Hit: 7 (58%)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting wooded-unit target1 board layout :attack/regular)) false) "To Hit: 6 (72%)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting attacker1 wooded-unit board layout :attack/regular)) false) "To Hit: 7 (58%)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting target1 attacker1 board layout :attack/regular)) false) "To Hit: 6 (72%)"))
    (:attack/regular (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting blinded-attacker blinded-target board layout :attack/regular)) false)
                              "Line of sight blocked")))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting blinded-target blinded-attacker board layout :attack/regular)) false)
             "Line of sight blocked")))
  (t/testing "Return full attack roll result"
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting attacker1 target1 board layout :attack/regular)) true)
             "To Hit: 6 (72%): + 4 (Pilot skill) + 0 (Fire-control damage) + 0 (Attacker moved) + 2 (Target movement) + 0 (Attacker heat) + 0 (Clear line of sight) + 0 (No intervening woods) + 0 (Target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting heated-attacker target1 board layout :attack/regular)) true)
             "To Hit: 7 (58%): + 4 (Pilot skill) + 0 (Fire-control damage) + 0 (Attacker moved) + 2 (Target movement) + 1 (Attacker heat) + 0 (Clear line of sight) + 0 (No intervening woods) + 0 (Target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting wooded-unit target1 board layout :attack/regular)) true)
             "To Hit: 6 (72%): + 4 (Pilot skill) + 0 (Fire-control damage) + 0 (Attacker moved) + 2 (Target movement) + 0 (Attacker heat) + 0 (Clear line of sight) + 0 (No intervening woods) + 0 (Target 2 hexes away)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting attacker1 wooded-unit board layout :attack/regular)) true)
             "To Hit: 7 (58%): + 4 (Pilot skill) + 0 (Fire-control damage) + 0 (Attacker moved) + 2 (Target movement) + 0 (Attacker heat) + 0 (Clear line of sight) + 1 (Target in woods) + 0 (Target 3 hexes away)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting target1 attacker1 board layout :attack/regular)) true)
             "To Hit: 6 (72%): + 4 (Pilot skill) + 0 (Fire-control damage) + 0 (Attacker moved) + 2 (Target movement) + 0 (Attacker heat) + 0 (Clear line of sight) + 0 (No intervening woods) + 0 (Target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting blinded-attacker blinded-target board layout :attack/regular)) true)
             "Line of sight blocked"))
    (t/is (= (sut/print-attack-roll (:attack/regular (sut/->targeting blinded-target blinded-attacker board layout :attack/regular)) true)
             "Line of sight blocked"))))

(t/deftest test-attack-confirmation-choices
  (t/testing "Tests attacks"
    (t/is (= (sut/attack-confirmation-choices attacker1 target1 board layout)
             {:attack/charge
              {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "No heat applied", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/charge, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "1", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"},
              :attack/physical {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "No heat applied", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/physical, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "1", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"},
              :attack/rear {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/rear, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "1", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"},
              :attack/regular {:targeting/attack-data {:targeting/amm {:targeting/description "Attacker moved", :targeting/value 0}, :targeting/fc-damage {:targeting/description "Fire-control damage", :targeting/value 0}, :targeting/heat {:targeting/description "Attacker heat", :targeting/value 0}, :targeting/los {:targeting/description "Clear line of sight", :targeting/value 0}, :targeting/range-mod {:targeting/description "Target 1 hexes away", :targeting/value 0}, :targeting/skill {:targeting/description "Pilot skill", :targeting/value 4}, :targeting/tmm {:targeting/description "Target movement", :targeting/value 2}, :targeting/woods {:targeting/description "No intervening woods", :targeting/value 0}}, :targeting/attack-type :attack/regular, :targeting/attacker "Wolfhound WLF-2", :targeting/damage "3", :targeting/distance 1, :targeting/rear-attack? false, :targeting/target "Wolfhound WLF-2"}}))))

; (t/deftest test-take-damage
;   (t/testing "Test armor only damage."
;     (t/is (= (sut/take-damage attacker1 2)
;              {:crit [nil nil]
;               :result (assoc attacker1 :changes {:current-armor 2 :current-structure 3})}))
;     (t/is (= (sut/take-damage attacker1 3)
;              {:crit [nil nil]
;               :result (assoc attacker1 :changes {:current-armor 1 :current-structure 3})}))
;     (t/is (= (sut/take-damage attacker1 0)
;              {:crit [nil nil]
;               :result attacker1}))
;     (t/is (= (sut/take-damage attacker1 4)
;              {:crit [nil nil]
;               :result (assoc attacker1 :changes {:current-armor 0 :current-structure 3})})))
;   (t/testing "Test penetration."
;     (let [tgt (:result (sut/take-damage attacker1 5))]
;       (t/is (= (get-in tgt [:changes :current-armor]) 0))
;       (t/is (= (get-in tgt [:changes :current-structure]) 2)))))
;
; (t/deftest test-make-attack
;   (t/testing "Test attack that hits"
;     (t/is (= (sut/make-attack (merge attacker1 {:direction :n}) (merge target1 {:direction :n}) board (hex/create-layout) 11)
;              {:targeting-data
;               {:targeting [[{:desc "pilot skill", :value 4}]
;                            [{:desc "0 fire control hits", :value 0}]
;                            [{:desc "attacker moved", :value 0}]
;                            [{:desc "target moved", :value 2}]
;                            nil
;                            [{:desc "clear line of sight", :value 0}]
;                            [{:desc "no intervening woods", :value 0}]
;                            [{:desc "target 1 hexes away", :value 0}]],
;                :damage "3"},
;               :rear-attack? false,
;               :to-hit 11,
;               :attacker {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "", :e* false, :movement {:walk 6}, :r -2, :right-arc "", :pilot {:name " Lieutenant Ciro Ramirez", :skill 4}, :force :1stsomersetstrikers, :mul-id 3563, :l* false, :m 3, :type "BM", :front-arc "", :current-structure 3, :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3, :threshold -1, :l 1, :size 1, :m* false, :rear-arc "", :point-value 28, :overheat 0, :chassis "Wolfhound", :structure 3, :crits [], :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2", :armor 4, :current-heat 0, :current-armor 4, :s* false, :p 1, :movement-mode :walk, :direction :n, :changes {}, :model "WLF-2"},
;               :damage 3,
;               :target {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "",
;                        :e* false, :movement {:walk 6}, :r -3, :right-arc "",
;                        :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
;                        :force :1stsomersetstrikers, :mul-id 3563, :l* false,
;                        :m 3, :type "BM", :front-arc "",
;                        :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3,
;                        :threshold -1, :l 1, :size 1, :m* false, :rear-arc "",
;                        :point-value 28, :overheat 0, :chassis "Wolfhound",
;                        :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2",
;                        :current-heat 0, :s* false, :p 2, :movement-mode :walk,
;                        :armor 4, :current-armor 4,
;                        :current-structure 3, :structure 3, :crits [],
;                        :changes {}
;                        :direction :n, :model "WLF-2"},
;               :crit [nil nil],
;               :result {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "",
;                        :e* false, :movement {:walk 6}, :r -3, :right-arc "",
;                        :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
;                        :force :1stsomersetstrikers, :mul-id 3563, :l* false,
;                        :m 3, :type "BM", :front-arc "",
;                        :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3,
;                        :threshold -1, :l 1, :size 1, :m* false, :rear-arc "",
;                        :point-value 28, :overheat 0, :chassis "Wolfhound",
;                        :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2",
;                        :current-heat 0, :s* false, :p 2, :movement-mode :walk,
;                        :armor 4, :current-armor 4,
;                        :current-structure 3, :structure 3, :crits [],
;                        :changes {:current-armor 1 :current-structure 3},
;                        :direction :n, :model "WLF-2"}}))))
;
(t/deftest height-checker-test
  (t/testing "Check for LOS"
    (t/is (= (sut/height-checker
              attacker1 target1
              (board/line
               (board/find-hex (:unit/location attacker1) board)
               (board/find-hex (:unit/location target1) board) board)) false))
    (t/is (= (sut/height-checker
              attacker1 wooded-unit
              (board/line
               (board/find-hex (:unit/location attacker1) board)
               (board/find-hex (:unit/location wooded-unit) board) board)) false))
    (t/is (= (sut/height-checker
              blinded-attacker blinded-target
              (board/line
               (board/find-hex blinded-attacker board)
               (board/find-hex blinded-target board) board)) true))
    (t/is (= (sut/height-checker
              blinded-target blinded-attacker
              (board/line
               (board/find-hex blinded-target board)
               (board/find-hex blinded-attacker board) board)) true))
    (t/is (= (sut/height-checker
              heated-attacker target1
              (board/line
               (board/find-hex heated-attacker board)
               (board/find-hex target1 board) board)) false))
    (t/is (= (sut/height-checker
              heated-attacker wooded-unit
              (board/line
               (board/find-hex heated-attacker board)
               (board/find-hex wooded-unit board) board)) false))
    (t/is (= (sut/height-checker
              heated-attacker blinded-target
              (board/line
               (board/find-hex (:unit/location heated-attacker) board)
               (board/find-hex (:unit/location blinded-target) board) board)) false))))

