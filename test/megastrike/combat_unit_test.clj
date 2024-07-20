(ns megastrike.combat-unit-test
  (:require [megastrike.combat-unit :as sut]
            [clojure-csv.core :as csv]
            [clojure.test :as t]))

(t/deftest test-move-parser
  (t/testing "Valid Mv Strings" 
    (t/is (= (sut/parse-movement "8\\\"") {:walk 4}))
    (t/is (= (sut/parse-movement "8\\\"j") {:jump 4 :walk 4}))
    (t/is (= (sut/parse-movement "6\\\"t") {:t 3}))
    (t/is (= (sut/parse-movement "16\\\"/12\\\"j") {:walk 8 :jump 6}))))

(t/deftest test-mul-parser
  (let [bm-test "73	Archer	ARC-2K	Missile Boat	BM	3	8\\\"	1	6	6	-1	2	FALSE	2	FALSE	2	FALSE	0	FALSE	2	34	IF2"
        cv-test "4879	Puma Assault Tank	PAT-001	None	CV	4	6\\\"t	1	6	5	-1	4	FALSE	5	FALSE	3	FALSE	0	FALSE	0	37	IF2, LRM1/2/2, REAR1/-/-, SRCH, TUR(1/1/1)"
        sv-test "3684	Air Car		None	SV	2	22\\\"h	4	1	2	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	6	BAR, EE, ENE"
        pm-test "510	Centaur	2	Scout	PM	1	12\\\"	2	1	1	-1	1	FALSE	0	TRUE	0	FALSE	0	FALSE	0	10					"
        ci-test "-1	Motorized Sub Platoon	(Laser SCUBA)	Undetermined	CI	1	4\\\"s	0	1	1	-1	1	FALSE	1	FALSE	0	FALSE	0	FALSE	0	8	AM, CAR4, UMU				"
        im-test "4545	Lumberjack	LM1/A	Ambusher	IM	3	6\\\"	1	1	5	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	7	BAR, BFC, CT8, EE, ENE, MEL, SAW				"
        ba-test "952	Elemental Battle Armor	(Headhunter)(Sqd4)	Ambusher	BA	1	6\\\"j	1	1	2	-1	1	FALSE	0	FALSE	0	FALSE	0	FALSE	0	15	AM, CAR4, MEC, RCN, RSD1				"]
    (t/testing "Sample MUL rows"
      (t/is (= (sut/parse-row (first (csv/parse-csv bm-test :delimiter \tab))) {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv cv-test :delimiter \tab))) {:role "None", :tmm 1, :e* false, :movement {:t 3}, :mul-id 4879, :l* false, :m 5, :type "CV", :abilities "IF2, LRM1/2/2, REAR1/-/-, SRCH, TUR(1/1/1)", :e 0, :s 4, :threshold -1, :l 3, :size 4, :m* false, :point-value 37, :overheat 0, :chassis "Puma Assault Tank", :structure 5, :full-name "Puma Assault Tank PAT-001", :armor 6, :s* false, :model "PAT-001"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv sv-test :delimiter \tab))) {:role "None", :tmm 4, :e* false, :movement {:h 11}, :mul-id 3684, :l* false, :m 0, :type "SV", :abilities "BAR, EE, ENE", :e 0, :s 0, :threshold -1, :l 0, :size 2, :m* false, :point-value 6, :overheat 0, :chassis "Air Car", :structure 2, :full-name "Air Car ", :armor 1, :s* false, :model ""}))
      (t/is (= (sut/parse-row (first (csv/parse-csv pm-test :delimiter \tab))) {:role "Scout", :tmm 2, :left-arc "", :e* false, :movement {:walk 6}, :right-arc "", :mul-id 510, :l* false, :m 0, :type "PM", :front-arc "", :abilities "", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* true, :rear-arc "", :point-value 10, :overheat 0, :chassis "Centaur", :structure 1, :full-name "Centaur 2", :armor 1, :s* false, :model "2"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ci-test :delimiter \tab))) {:role "Undetermined", :tmm 0, :left-arc "", :e* false, :movement {:s 2}, :right-arc "", :mul-id -1, :l* false, :m 1, :type "CI", :front-arc "", :abilities "AM, CAR4, UMU", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 8, :overheat 0, :chassis "Motorized Sub Platoon", :structure 1, :full-name "Motorized Sub Platoon (Laser SCUBA)", :armor 1, :s* false, :model "(Laser SCUBA)"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv im-test :delimiter \tab))) {:role "Ambusher", :tmm 1, :left-arc "", :e* false, :movement {:walk 3}, :right-arc "", :mul-id 4545, :l* false, :m 0, :type "IM", :front-arc "", :abilities "BAR, BFC, CT8, EE, ENE, MEL, SAW", :e 0, :s 0, :threshold -1, :l 0, :size 3, :m* false, :rear-arc "", :point-value 7, :overheat 0, :chassis "Lumberjack", :structure 5, :full-name "Lumberjack LM1/A", :armor 1, :s* false, :model "LM1/A"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ba-test :delimiter \tab))) {:role "Ambusher", :tmm 1, :left-arc "", :e* false, :movement {:jump 3 :walk 3}, :right-arc "", :mul-id 952, :l* false, :m 0, :type "BA", :front-arc "", :abilities "AM, CAR4, MEC, RCN, RSD1", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 15, :overheat 0, :chassis "Elemental Battle Armor", :structure 2, :full-name "Elemental Battle Armor (Headhunter)(Sqd4)", :armor 1, :s* false, :model "(Headhunter)(Sqd4)"})))))

(t/deftest test-filters
  (t/testing "Filter by name"
    (t/is (= (:point-value (first (sut/filter-units sut/mul :full-name "Archer ARC-2K" =))) 34)))
  (t/testing "Empty filters return unaltered lists."
    (t/is (= (sut/filter-units sut/mul) sut/mul))))

(t/deftest test-print-movement
  (t/testing "Basic movement."
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K"}) "4"))
    (t/is (= (sut/print-movement {:role "Test Jumper" :movement {:jump 4 :walk 4}}) "4j/4"))
    (t/is (= (sut/print-movement {:role "Test Weak Jumper" :movement {:jump 6 :walk 4}}) "6j/4"))
    (t/is (= (sut/print-movement {:role "Test Strong Jumper" :movement {:jump 2 :walk 4}}) "2j/4")))
  (t/testing "Damaged movement."
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :crits [:mv]}) "2"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :crits [:mv :fire-control]}) "2"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :crits [:mv :mv :fire-control]}) "1"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :crits [:mv :mv :mv :fire-control]}) "0"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :movement {:walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :crits [:fire-control]}) "4"))))

(t/deftest test-create-element
  (t/testing "Verify new keys merged."
    (t/is (contains? (sut/create-element (first (sut/filter-units sut/mul :full-name "Archer ARC-2K" =))
                                        {:pilot {:name "Bobby McSkillface" :skill 4}}) :pilot)))
  (t/testing "Test Creation with units"
    (t/is (= (sut/create-element (sut/get-unit "Wolfhound WLF-2")
                                 {:id "Wolfhound WLF-2" :path [] :p 11 :q 5 :r -16 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}) 
             {:role "Striker", :path [], :tmm 2, :q 5, :left-arc "", :e* false, :movement {:walk 6}, :r -16, :right-arc "", :pilot {:name " Lieutenant Ciro Ramirez", :skill 4}, :force :1stsomersetstrikers, :mul-id 3563, :l* false, :m 3, :type "BM", :front-arc "", :current-structure 3, :abilities "ENE, REAR1/1/-", :acted nil, :e 0, :s 3, :threshold -1, :l 1, :size 1, :m* false, :rear-arc "", :point-value 28, :overheat 0, :chassis "Wolfhound", :structure 3, :crits [], :id "Wolfhound WLF-2", :full-name "Wolfhound WLF-2", :armor 4, :current-heat 0, :current-armor 4, :s* false, :p 11, :movement-mode :walk, :direction :s, :model "WLF-2"}))
    ;; This should be expanded much further to handle edge cases.
    ))

(t/deftest test-pv-mod-calculation
  (t/testing "Check PV Mod calculation."
    (t/is (= (sut/pv {:point-value 10 :pilot {:skill 4}}) 10))
    (t/is (= (sut/pv {:point-value 10 :pilot {:skill 3}}) 12))
    (t/is (= (sut/pv {:point-value 10 :pilot {:skill 5}}) 9))))

(t/deftest test-parse-mechset-line
  (t/testing "Test comment lines"
    (t/is (= (sut/parse-mechset-line "#") nil))
    (t/is (= (sut/parse-mechset-line "# Test comment") nil)))
  (t/testing "Test empty lines"
    (t/is (= (sut/parse-mechset-line "") nil)))
  (t/testing "Test include lines"
    (t/is (= (sut/parse-mechset-line "include this/file/here.txt") nil)))
  (t/testing "Test functional lines"
    (t/is (= (sut/parse-mechset-line "exact \"default_quadvee\" \"defaults/default_quadvee.png\"")
             ["exact" "default_quadvee" "defaults/default_quadvee.png"]))
    (t/is (= (sut/parse-mechset-line "chassis \"Ahab\" \"fighter/ahab.png\"")
             ["chassis" "Ahab" "fighter/ahab.png"]))
    (t/is (= (sut/parse-mechset-line "exact \"Archer ARC-2K\" \"mechs/Archer_2K.png\"")
             ["exact" "Archer ARC-2K" "mechs/Archer_2K.png"]))))

;; TODO Find a platform-independent way of testing if two files are the same.
;; (t/deftest test-find-sprite
;;   (t/testing "Test searching for a valid sprite."
;;     (t/is (= (sut/find-sprite {:full-name "Archer ARC-2K" :chassis "Archer"}) "resources/images/units/mechs/Archer_2K.png"))
;;     (t/is (= (sut/find-sprite {:full-name "Ahab AHB-4" :chassis "Ahab"}) "resources/images/units/fighter/ahab.png"))))

(t/deftest test-get-units
  (t/testing "Get units for scenarios."
    ;; Known failing tests commented out until its time to address them.
    ;; These tests are just here to document the relevant issue. Since they all involve units
    ;; which are not Mechs, they will not be addressed until all the Quick Start rules are implemented
    ;; (t/is (= (sut/get-unit "Sloth Battle Armor (Standard)") 1))
    ;; (t/is (= (sut/get-unit "Foot Platoon (Laser)") {:role "Ambusher", :tmm 0, :left-arc "", :e* false, :movement {:f 1}, :right-arc "", :mul-id 1144, :l* false, :m 1, :type "CI", :front-arc "", :abilities "AM, CAR3", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 9, :overheat 0, :chassis "Foot Platoon", :structure 1, :full-name "Foot Platoon (Laser)", :armor 2, :s* false, :model "(Laser)"}))
    ;; (t/is (= (sut/get-unit "Elemental Battle Armor [Laser]") 1))
    ;; (t/is (= (sut/get-unit "Elemental Battle Armor [Flamer]") 1))
    ;; (t/is (= (sut/get-unit "Elemental Battle Armor [MG]") 1))
    (t/is (= (sut/get-unit "Firestarter FS9-H") {:role "Scout", :tmm 2, :left-arc "", :e* false, :movement {:jump 6, :walk 6}, :right-arc "", :mul-id 1096, :l* false, :m 1, :type "BM", :front-arc "", :abilities "HT1/-/-, REAR0*/-/-", :e 0, :s 2, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 20, :overheat 0, :chassis "Firestarter", :structure 3, :full-name "Firestarter FS9-H", :armor 3, :s* false, :model "FS9-H"}))
    ;; (t/is (= (sut/get-unit "Gnome Battle Armor (Standard)") 0))
    ;; (t/is (= (sut/get-unit "Clan Foot Point (Laser)") {:role "Ambusher", :tmm 0, :left-arc "", :e* false, :movement {:f 1}, :right-arc "", :mul-id 603, :l* false, :m 1, :type "CI", :front-arc "", :abilities "AM, CAR3", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 11, :overheat 0, :chassis "Clan Foot Point", :structure 1, :full-name "Clan Foot Point (Laser)", :armor 3, :s* false, :model "(Laser)"}))
    ))

(t/deftest test-calculate-damage
  (t/testing "Test damage without a *."
    (t/is (= (sut/calculate-damage {:id "Unit 1" :s 4} 2 false) 4))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :m 4} 4 false) 4))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :l 4} 13 false) 4))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :e 4} 22 false) 4))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :s 4} 2 true) 5))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :m 4} 4 true) 5))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :l 4} 13 true) 5))
    (t/is (= (sut/calculate-damage {:id "Unit 1" :e 4} 22 true) 5)))
  (t/testing "Test Damage with a *." 
    (let [s-damage (sut/calculate-damage {:id "Unit 1" :s 0 :s* true} 2 false)
          m-damage (sut/calculate-damage {:id "Unit 1" :m 0 :m* true} 9 false)
          l-damage (sut/calculate-damage {:id "Unit 1" :l 0 :l* true} 13 false)
          e-damage (sut/calculate-damage {:id "Unit 1" :e 0 :e* true} 22 false)
          sr-damage (sut/calculate-damage {:id "Unit 1" :s 0 :s* true} 2 true)
          mr-damage (sut/calculate-damage {:id "Unit 1" :m 0 :m* true} 9 true)
          lr-damage (sut/calculate-damage {:id "Unit 1" :l 0 :l* true} 13 true)
          er-damage (sut/calculate-damage {:id "Unit 1" :e 0 :e* true} 22 true)] 
      (t/is (or (= s-damage 0) (= s-damage 1))) 
      (t/is (or (= m-damage 0) (= m-damage 1))) 
      (t/is (or (= l-damage 0) (= l-damage 1))) 
      (t/is (or (= e-damage 0) (= e-damage 1)))
      (t/is (or (= sr-damage 1) (= sr-damage 2))) 
      (t/is (or (= mr-damage 1) (= mr-damage 2))) 
      (t/is (or (= lr-damage 1) (= lr-damage 2))) 
      (t/is (or (= er-damage 1) (= er-damage 2)))
      )))
