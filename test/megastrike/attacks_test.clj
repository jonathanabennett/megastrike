(ns megastrike.attacks-test
  (:require
   [clojure.test :as t]
   [megastrike.attacks :as sut]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.hexagons.hex :as hex]))

(def board (board/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board"))
(def attacker1 (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 1 :q 1 :r -2 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))
(def target1 (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 2 :q 1 :r -3 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))
(def wooded-unit (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 4 :q 0 :r -4 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))
(def blinded-attacker (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 3 :q 4 :r -7 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))
(def blinded-target (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 7 :q 2 :r -9 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))
(def heated-attacker (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 1 :q 1 :r -2 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 1 :current-armor 4 :movement-mode :walk :direction :s}))

(t/deftest attacker-skill-test
  (t/testing "Get the attacker skill"
    (t/is (= (sut/attacker-skill {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4}}) [{:desc "pilot skill" :value 4}]))))

(t/deftest calculate-amm-test
  (t/testing "Get the attacker's movement mod"
    (t/is (= (sut/calculate-amm {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4} :movement-mode :immobile}) [{:desc "attacker immobile" :value -1}]))
    (t/is (= (sut/calculate-amm {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4} :movement-mode :stand-still}) [{:desc "attacker stood still" :value -1}]))
    (t/is (= (sut/calculate-amm {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4} :movement-mode :jump}) [{:desc "attacker jumped" :value 2}]))
    (t/is (= (sut/calculate-amm {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4} :movement-mode :walk}) [{:desc "attacker moved" :value 0}]))
    (t/is (= (sut/calculate-amm {:id "Test Unit" :pilot {:name "Test Pilot" :skill 4} :movement-mode :hover}) [{:desc "attacker moved" :value 0}]))))

(t/deftest calculate-fc-hits-test
  (t/testing "Calculate Fire Control hits"
    (t/is (= (sut/calculate-fc-hits {:id "Test Unit" :crits []}) [{:desc "0 fire control hits" :value 0}]))
    (t/is (= (sut/calculate-fc-hits {:id "Test Unit" :crits [:mv]}) [{:desc "0 fire control hits" :value 0}]))
    (t/is (= (sut/calculate-fc-hits {:id "Test Unit" :crits [:mv :fire-control]}) [{:desc "1 fire control hit" :value 2}]))
    (t/is (= (sut/calculate-fc-hits {:id "Test Unit" :crits [:fire-control :fire-control]}) [{:desc "2 fire control hits" :value 4}]))))

(t/deftest calculate-target-mod-test
  (t/testing "Calculate TMM Mod"
    (t/is (= (sut/calculate-target-mod {:id "Test Unit" :movement-mode :immobile :tmm 4}) [{:desc "target immobile" :value -4}]))
    (t/is (= (sut/calculate-target-mod {:id "Test Unit" :movement-mode :stand-still :tmm 4}) [{:desc "target did not move" :value 0}]))
    (t/is (= (sut/calculate-target-mod {:id "Test Unit" :movement-mode :jump :tmm 4}) [{:desc "target jumped" :value 5}]))
    (t/is (= (sut/calculate-target-mod {:id "Test Unit" :movement-mode :walk :tmm 4}) [{:desc "target moved" :value 4}]))
    (t/is (= (sut/calculate-target-mod {:id "Test Unit" :movement-mode :walk :tmm 4 :crits [:mv]}) [{:desc "target moved" :value 2}]))))

(t/deftest calculate-heat-mod-test
  (t/testing "Calculate heat modifier"
    (t/is (= (sut/calculate-heat-mod {:id "Test unit"}) nil) "Missing heat")
    (t/is (= (sut/calculate-heat-mod {:id "Test unit" :current-heat 0}) nil) "Zero heat")
    (t/is (= (sut/calculate-heat-mod {:id "Test unit" :current-heat 1}) [{:desc "attacker heat" :value 1}]))
    (t/is (= (sut/calculate-heat-mod {:id "Test unit" :current-heat 2}) [{:desc "attacker heat" :value 2}]))
    (t/is (= (sut/calculate-heat-mod {:id "Test unit" :current-heat 3}) [{:desc "attacker heat" :value 3}]))))

(t/deftest woods-mod-test
  (let [woods-hex {:p 8, :q 6, :r -14, :elevation 0, :terrain "woods:1:20;ground_fluff:1:2;foliage_elev:2", :palette "grass"}
        empty-hex {:p 12, :q 4, :r -16, :elevation 0, :terrain "", :palette "grass"}
        rough-hex {:p 13, :q 4, :r -17, :elevation 0, :terrain "rough:1:20", :palette "grass"}
        ground-fluff-hex {:p 2, :q 9, :r -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}]
    (t/testing "Calculate woods mod"
      (t/is (= (sut/woods-mod [empty-hex rough-hex ground-fluff-hex]) [{:desc "no intervening woods" :value 0}]) "Testing no woods at all")
      (t/is (= (sut/woods-mod [woods-hex empty-hex empty-hex]) [{:desc "no intervening woods" :value 0}]) "Testing attacker standing in woods.")
      (t/is (= (sut/woods-mod [empty-hex woods-hex empty-hex]) [{:desc "intervening woods" :value 1}]))
      (t/is (= (sut/woods-mod [empty-hex empty-hex woods-hex]) [{:desc "target in woods" :value 1}]))
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex]) [{:desc "target in woods" :value 1}]))
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex empty-hex]) [{:desc "intervening woods" :value 1}]))
      (t/is (= (sut/woods-mod [empty-hex woods-hex woods-hex woods-hex woods-hex]) [{:desc "Line of Sight blocked by woods" :value ##Inf}])))))

(t/deftest calculate-range-mod-test
  (t/testing "Calculate Range mod"
    (t/is (= (sut/calculate-range-mod 1)  [{:desc "target 1 hexes away"  :value 0}]))
    (t/is (= (sut/calculate-range-mod 2)  [{:desc "target 2 hexes away"  :value 0}]))
    (t/is (= (sut/calculate-range-mod 3)  [{:desc "target 3 hexes away"  :value 0}]))
    (t/is (= (sut/calculate-range-mod 4)  [{:desc "target 4 hexes away"  :value 2}]))
    (t/is (= (sut/calculate-range-mod 5)  [{:desc "target 5 hexes away"  :value 2}]))
    (t/is (= (sut/calculate-range-mod 11) [{:desc "target 11 hexes away" :value 2}]))
    (t/is (= (sut/calculate-range-mod 12) [{:desc "target 12 hexes away" :value 2}]))
    (t/is (= (sut/calculate-range-mod 13) [{:desc "target 13 hexes away" :value 4}]))
    (t/is (= (sut/calculate-range-mod 14) [{:desc "target 14 hexes away" :value 4}]))
    (t/is (= (sut/calculate-range-mod 15) [{:desc "target 15 hexes away" :value 4}]))
    (t/is (= (sut/calculate-range-mod 20) [{:desc "target 20 hexes away" :value 4}]))
    (t/is (= (sut/calculate-range-mod 21) [{:desc "target 21 hexes away" :value 4}]))
    (t/is (= (sut/calculate-range-mod 22) [{:desc "target 22 hexes away" :value 6}]))
    (t/is (= (sut/calculate-range-mod 29) [{:desc "target 29 hexes away" :value 6}]))
    (t/is (= (sut/calculate-range-mod 30) [{:desc "target 30 hexes away" :value 6}]))
    (t/is (= (sut/calculate-range-mod 31) [{:desc "Target out of range" :value ##Inf}]))))

(t/deftest height-checker-test
  (t/testing "Check for LOS"
    (t/is (= (sut/height-checker
              attacker1 target1
              (board/line
               (board/find-hex attacker1 board)
               (board/find-hex target1 board) board)) false))
    (t/is (= (sut/height-checker
              attacker1 wooded-unit
              (board/line
               (board/find-hex attacker1 board)
               (board/find-hex wooded-unit board) board)) false))
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
               (board/find-hex heated-attacker board)
               (board/find-hex blinded-target board) board)) false))))

(t/deftest produce-attack-roll-test
  (t/testing "Testing attack rolls"
    (t/is (= (sut/produce-attack-roll attacker1 target1 board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "clear line of sight", :value 0}] [{:desc "no intervening woods", :value 0}] [{:desc "target 1 hexes away", :value 0}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll heated-attacker target1 board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] [{:desc "attacker heat", :value 1}] [{:desc "clear line of sight", :value 0}] [{:desc "no intervening woods", :value 0}] [{:desc "target 1 hexes away", :value 0}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll wooded-unit target1 board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "clear line of sight", :value 0}] [{:desc "no intervening woods", :value 0}] [{:desc "target 2 hexes away", :value 0}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll attacker1 wooded-unit board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "clear line of sight", :value 0}] [{:desc "target in woods", :value 1}] [{:desc "target 3 hexes away", :value 0}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll target1 attacker1 board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "clear line of sight", :value 0}] [{:desc "no intervening woods", :value 0}] [{:desc "target 1 hexes away", :value 0}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll blinded-attacker blinded-target board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "Line of Sight Blocked", :value ##Inf}] [{:desc "no intervening woods", :value 0}] [{:desc "target 4 hexes away", :value 2}]]
              :damage "3"}))
    (t/is (= (sut/produce-attack-roll blinded-target blinded-attacker board :regular)
             {:targeting [[{:desc "pilot skill", :value 4}] [{:desc "0 fire control hits", :value 0}] [{:desc "attacker moved", :value 0}] [{:desc "target moved", :value 2}] nil [{:desc "Line of Sight Blocked", :value ##Inf}] [{:desc "no intervening woods", :value 0}] [{:desc "target 4 hexes away", :value 2}]]
              :damage "3"}))))

(t/deftest calculate-to-hit-test
  (t/testing "return to-hit numbers for attacks"
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll attacker1 target1 board :regular)) 6))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll heated-attacker target1 board :regular)) 7))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll wooded-unit target1 board :regular)) 6))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll attacker1 wooded-unit board :regular)) 7))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll target1 attacker1 board :regular)) 6))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll blinded-attacker blinded-target board :regular)) ##Inf))
    (t/is (= (sut/calculate-to-hit (sut/produce-attack-roll blinded-target blinded-attacker board :regular)) ##Inf))))

(t/deftest print-attack-roll
  (t/testing "Return minimum attack roll result"
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll attacker1 target1 board :regular) false) "To Hit: 6 (72%)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll heated-attacker target1 board :regular) false) "To Hit: 7 (58%)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll wooded-unit target1 board :regular) false) "To Hit: 6 (72%)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll attacker1 wooded-unit board :regular) false) "To Hit: 7 (58%)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll target1 attacker1 board :regular) false) "To Hit: 6 (72%)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll blinded-attacker blinded-target board :regular) false)
             "Line of Sight Blocked"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll blinded-target blinded-attacker board :regular) false)
             "Line of Sight Blocked")))
  (t/testing "Return full attack roll result"
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll attacker1 target1 board :regular) true)
             "To Hit: 6 (72%): + 4 (pilot skill) + 0 (0 fire control hits) + 0 (attacker moved) + 2 (target moved) + 0 () + 0 (clear line of sight) + 0 (no intervening woods) + 0 (target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll heated-attacker target1 board :regular) true)
             "To Hit: 7 (58%): + 4 (pilot skill) + 0 (0 fire control hits) + 0 (attacker moved) + 2 (target moved) + 1 (attacker heat) + 0 (clear line of sight) + 0 (no intervening woods) + 0 (target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll wooded-unit target1 board :regular) true)
             "To Hit: 6 (72%): + 4 (pilot skill) + 0 (0 fire control hits) + 0 (attacker moved) + 2 (target moved) + 0 () + 0 (clear line of sight) + 0 (no intervening woods) + 0 (target 2 hexes away)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll attacker1 wooded-unit board :regular) true)
             "To Hit: 7 (58%): + 4 (pilot skill) + 0 (0 fire control hits) + 0 (attacker moved) + 2 (target moved) + 0 () + 0 (clear line of sight) + 1 (target in woods) + 0 (target 3 hexes away)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll target1 attacker1 board :regular) true)
             "To Hit: 6 (72%): + 4 (pilot skill) + 0 (0 fire control hits) + 0 (attacker moved) + 2 (target moved) + 0 () + 0 (clear line of sight) + 0 (no intervening woods) + 0 (target 1 hexes away)"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll blinded-attacker blinded-target board :regular) true)
             "Line of Sight Blocked"))
    (t/is (= (sut/print-attack-roll (sut/produce-attack-roll blinded-target blinded-attacker board :regular) true)
             "Line of Sight Blocked"))))

(t/deftest test-take-damage
  (t/testing "Test armor only damage."
    (t/is (= (sut/take-damage attacker1 2)
             {:crit [nil nil]
              :result (assoc attacker1 :changes {:current-armor 2 :current-structure 3})}))
    (t/is (= (sut/take-damage attacker1 3)
             {:crit [nil nil]
              :result (assoc attacker1 :changes {:current-armor 1 :current-structure 3})}))
    (t/is (= (sut/take-damage attacker1 0)
             {:crit [nil nil]
              :result attacker1}))
    (t/is (= (sut/take-damage attacker1 4)
             {:crit [nil nil]
              :result (assoc attacker1 :changes {:current-armor 0 :current-structure 3})})))
  (t/testing "Test penetration."
    (let [tgt (:result (sut/take-damage attacker1 5))]
      (t/is (= (get-in tgt [:changes :current-armor]) 0))
      (t/is (= (get-in tgt [:changes :current-structure]) 2)))))

(t/deftest test-make-attack
  (t/testing "Test attack that hits"
    (t/is (= (sut/make-attack (merge attacker1 {:direction :n}) (merge target1 {:direction :n}) board (hex/create-layout) 11)
             {:targeting-data
              {:targeting [[{:desc "pilot skill", :value 4}]
                           [{:desc "0 fire control hits", :value 0}]
                           [{:desc "attacker moved", :value 0}]
                           [{:desc "target moved", :value 2}]
                           nil
                           [{:desc "clear line of sight", :value 0}]
                           [{:desc "no intervening woods", :value 0}]
                           [{:desc "target 1 hexes away", :value 0}]],
               :damage "3"},
              :rear-attack? false,
              :to-hit 11,
              :attacker {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "", :e* false, :movement {:walk 6}, :r -2, :right-arc "", :pilot {:name " Lieutenant Ciro Ramirez", :skill 4}, :force :1stsomersetstrikers, :mul-id 3563, :l* false, :m 3, :type "BM", :front-arc "", :current-structure 3, :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3, :threshold -1, :l 1, :size 1, :m* false, :rear-arc "", :point-value 28, :overheat 0, :chassis "Wolfhound", :structure 3, :crits [], :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2", :armor 4, :current-heat 0, :current-armor 4, :s* false, :p 1, :movement-mode :walk, :direction :n, :changes {}, :model "WLF-2"},
              :damage 3,
              :target {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "",
                       :e* false, :movement {:walk 6}, :r -3, :right-arc "",
                       :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
                       :force :1stsomersetstrikers, :mul-id 3563, :l* false,
                       :m 3, :type "BM", :front-arc "",
                       :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3,
                       :threshold -1, :l 1, :size 1, :m* false, :rear-arc "",
                       :point-value 28, :overheat 0, :chassis "Wolfhound",
                       :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2",
                       :current-heat 0, :s* false, :p 2, :movement-mode :walk,
                       :armor 4, :current-armor 4,
                       :current-structure 3, :structure 3, :crits [],
                       :changes {}
                       :direction :n, :model "WLF-2"},
              :crit [nil nil],
              :result {:role "Striker", :path [], :tmm 2, :q 1, :left-arc "",
                       :e* false, :movement {:walk 6}, :r -3, :right-arc "",
                       :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
                       :force :1stsomersetstrikers, :mul-id 3563, :l* false,
                       :m 3, :type "BM", :front-arc "",
                       :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3,
                       :threshold -1, :l 1, :size 1, :m* false, :rear-arc "",
                       :point-value 28, :overheat 0, :chassis "Wolfhound",
                       :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2",
                       :current-heat 0, :s* false, :p 2, :movement-mode :walk,
                       :armor 4, :current-armor 4,
                       :current-structure 3, :structure 3, :crits [],
                       :changes {:current-armor 1 :current-structure 3},
                       :direction :n, :model "WLF-2"}}))))

