(ns megastrike.combat-unit-test
  (:require
   [clojure-csv.core :as csv]
   [clojure.spec.alpha :as s]
   [clojure.test :as t]
   [megastrike.combat-unit :as sut]))

(def test-bm {:unit/abilities {:if {:value 2, :value* false, :ability/output "IF2"}},
              :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge},
                             :attack/physical {:attack/damage 3, :attack/self false, :attack/type :attack/physical},
                             :attack/regular {:attack/e 0, :attack/e* false, :attack/l 2, :attack/l* false, :attack/m 2, :attack/m* false, :attack/s 2, :attack/s* false}},
              :unit/base-pv 34,
              :unit/chassis "Archer",
              :unit/full-name "Archer ARC-2K",
              :unit/model "ARC-2K",
              :unit/move-modes {:move/walk 4},
              :unit/mul-id 73,
              :unit/overheat 2,
              :unit/role :role/missile-boat,
              :unit/size 3,
              :unit/armor {:toughness/current 6, :toughness/maximum 6, :toughness/unapplied 0},
              :unit/structure {:toughness/current 6, :toughness/maximum 6, :toughness/unapplied 0},
              :unit/threshold -1,
              :unit/tmm 1,
              :unit/type :type/bm})

(t/deftest test-move-parser
  (t/testing "Valid Mv Strings"
    (t/is (= (sut/parse-movement "8\\\"") {:move/walk 4}))
    (t/is (= (sut/parse-movement "8\\\"j") {:move/jump 4 :move/walk 4}))
    (t/is (= (sut/parse-movement "6\\\"t") {:move/t 3}))
    (t/is (= (sut/parse-movement "16\\\"/12\\\"j") {:move/walk 8 :move/jump 6}))))

(t/deftest test-mul-parser
  (let [bm-test "73	Archer	ARC-2K	Missile Boat	BM	3	8\\\"	1	6	6	-1	2	FALSE	2	FALSE	2	FALSE	0	FALSE	2	34	IF2"
        cv-test "4879	Puma Assault Tank	PAT-001	None	CV	4	6\\\"t	1	6	5	-1	4	FALSE	5	FALSE	3	FALSE	0	FALSE	0	37	IF2, LRM1/2/2, REAR1/-/-, SRCH, TUR(1/1/1)"
        sv-test "3684	Air Car		None	SV	2	22\\\"h	4	1	2	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	6	BAR, EE, ENE"
        pm-test "510	Centaur	2	Scout	PM	1	12\\\"	2	1	1	-1	1	FALSE	0	TRUE	0	FALSE	0	FALSE	0	10					"
        ci-test "-1	Motorized Sub Platoon	(Laser SCUBA)	Undetermined	CI	1	4\\\"s	0	1	1	-1	1	FALSE	1	FALSE	0	FALSE	0	FALSE	0	8	AM, CAR4, UMU				"
        im-test "4545	Lumberjack	LM1/A	Ambusher	IM	3	6\\\"	1	1	5	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	7	BAR, BFC, CT8, EE, ENE, MEL, SAW				"
        ba-test "952	Elemental Battle Armor	(Headhunter)(Sqd4)	Ambusher	BA	1	6\\\"j	1	1	2	-1	1	FALSE	0	FALSE	0	FALSE	0	FALSE	0	15	AM, CAR4, MEC, RCN, RSD1				"]
    (t/testing "Sample MUL rows"
      (t/is (= (sut/parse-row (first (csv/parse-csv bm-test :delimiter \tab)))
               test-bm))
      (t/is (= (sut/parse-row (first (csv/parse-csv cv-test :delimiter \tab)))
               {:unit/abilities {:if {:value 2, :value* false, :ability/output "IF2"}, :srch {:ability/output "SRCH"}, :tur {:ability/output "TUR(1/1/1)"}, :attack/lrm {:l 2, :l* false, :m 2, :m* false, :s 1, :s* false, :ability/output "LRM1/2/2"}, :attack/rear {:l 0, :l* false, :m 0, :m* false, :s 1, :s* false, :ability/output "REAR1/-/-"}}, :unit/armor {:toughness/current 6, :toughness/maximum 6, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/lrm {:attack/e 0, :attack/e* false, :attack/l 2, :attack/l* false, :attack/m 2, :attack/m* false, :attack/s 1, :attack/s* false}, :attack/physical {:attack/damage 4, :attack/self false, :attack/type :attack/physical}, :attack/rear {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 1, :attack/s* false}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 3, :attack/l* false, :attack/m 5, :attack/m* false, :attack/s 4, :attack/s* false}}, :unit/base-pv 37, :unit/chassis "Puma Assault Tank", :unit/full-name "Puma Assault Tank PAT-001", :unit/model "PAT-001", :unit/move-modes {:move/t 3}, :unit/mul-id 4879, :unit/overheat 0, :unit/role :role/none, :unit/size 4, :unit/structure {:toughness/current 6, :toughness/maximum 6, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 1, :unit/type :type/cv}))
      (t/is (= (sut/parse-row (first (csv/parse-csv sv-test :delimiter \tab)))
               {:unit/abilities {:bar {:ability/output "BAR"}, :ee {:ability/output "EE"}, :ene {:ability/output "ENE"}}, :unit/armor {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/physical {:attack/damage 2, :attack/self false, :attack/type :attack/physical}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 0, :attack/s* false}}, :unit/base-pv 6, :unit/chassis "Air Car", :unit/full-name "Air Car ", :unit/model "", :unit/move-modes {:move/h 11}, :unit/mul-id 3684, :unit/overheat 0, :unit/role :role/none, :unit/size 2, :unit/structure {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 4, :unit/type :type/sv}))
      (t/is (= (sut/parse-row (first (csv/parse-csv pm-test :delimiter \tab)))
               {:unit/abilities {:unknown {:ability/output ""}}, :unit/armor {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/physical {:attack/damage 1, :attack/self false, :attack/type :attack/physical}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 1, :attack/s* false}}, :unit/base-pv 10, :unit/chassis "Centaur", :unit/full-name "Centaur 2", :unit/model "2", :unit/move-modes {:move/walk 6}, :unit/mul-id 510, :unit/overheat 0, :unit/role :role/scout, :unit/size 1, :unit/structure {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 2, :unit/type :type/pm}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ci-test :delimiter \tab)))
               {:unit/abilities {:am {:ability/output "AM"}, :car {:value 1, :ability/output "CAR4"}, :umu {:ability/output "UMU"}}, :unit/armor {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/physical {:attack/damage 1, :attack/self false, :attack/type :attack/physical}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 1, :attack/m* false, :attack/s 1, :attack/s* false}}, :unit/base-pv 8, :unit/chassis "Motorized Sub Platoon", :unit/full-name "Motorized Sub Platoon (Laser SCUBA)", :unit/model "(Laser SCUBA)", :unit/move-modes {:move/s 2}, :unit/mul-id -1, :unit/overheat 0, :unit/role :role/undetermined, :unit/size 1, :unit/structure {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 0, :unit/type :type/ci}))
      (t/is (= (sut/parse-row (first (csv/parse-csv im-test :delimiter \tab)))
               {:unit/abilities {:bar {:ability/output "BAR"}, :bfc {:ability/output "BFC"}, :ct {:value 1, :ability/output "CT8"}, :ee {:ability/output "EE"}, :ene {:ability/output "ENE"}, :mel {:ability/output "MEL"}, :saw {:ability/output "SAW"}}, :unit/armor {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/physical {:attack/damage 4, :attack/self false, :attack/type :attack/physical}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 0, :attack/s* false}}, :unit/base-pv 7, :unit/chassis "Lumberjack", :unit/full-name "Lumberjack LM1/A", :unit/model "LM1/A", :unit/move-modes {:move/walk 3}, :unit/mul-id 4545, :unit/overheat 0, :unit/role :role/ambusher, :unit/size 3, :unit/structure {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 1, :unit/type :type/im}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ba-test :delimiter \tab)))
               {:unit/abilities {:am {:ability/output "AM"}, :car {:value 1, :ability/output "CAR4"}, :mec {:ability/output "MEC"}, :rcn {:ability/output "RCN"}, :rsd {:value 1, :ability/output "RSD1"}}, :unit/armor {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/physical {:attack/damage 1, :attack/self false, :attack/type :attack/physical}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 1, :attack/s* false}}, :unit/base-pv 15, :unit/chassis "Elemental Battle Armor", :unit/full-name "Elemental Battle Armor (Headhunter)(Sqd4)", :unit/model "(Headhunter)(Sqd4)", :unit/move-modes {:move/jump 3, :move/walk 3}, :unit/mul-id 952, :unit/overheat 0, :unit/role :role/ambusher, :unit/size 1, :unit/structure {:toughness/current 1, :toughness/maximum 1, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 1, :unit/type :type/ba})))))

(t/deftest test-filters
  (t/testing "Filter by name"
    (t/is (= (:unit/base-pv (first (sut/filter-units sut/mul :unit/full-name "Archer ARC-2K" =))) 34)))
  (t/testing "Filter by unit type"
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :type/bm))) :mul/mechs))
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :mul/mechs))) :type/bm))
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :mul/vehicle))) :mul/vehicle))
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :mul/conventional))) :mul/conventional))
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :mul/ground-units))) :mul/ground-units))
    (t/is (isa? (:unit/type (first (sut/filter-units sut/mul :mul/aero))) :mul/aero))
    (t/is (= (:unit/type (first (sut/filter-units sut/mul :type/bm))) :type/bm)))
  (t/testing "Empty filters return unaltered lists."
    (t/is (= (sut/filter-units sut/mul) sut/mul))))

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

(t/deftest test-create-element
  (t/testing "Verify new keys merged."
    (let [test-element (sut/->combat-unit (first (sut/filter-units sut/mul :unit/full-name "Archer ARC-2K" =))
                                          {:pilot/full-name "Bobby McSkillface" :pilot/skill 4 :pilot/kills 0} :direction/n
                                          {:hex/p 2 :hex/q 0 :hex/r -2} :davion 0)]
      (t/is (= (:unit/pilot test-element) {:pilot/full-name "Bobby McSkillface" :pilot/skill 4 :pilot/kills 0}))
      (t/is (= (:unit/battle-force test-element) :davion)))))

(t/deftest test-pv-mod-calculation
  (t/testing "Check PV Mod calculation."
    (t/is (= (sut/pv {:unit/base-pv 10 :unit/pilot {:pilot/skill 4}}) 10))
    (t/is (= (sut/pv {:unit/base-pv 10 :unit/pilot {:pilot/skill 3}}) 12))
    (t/is (= (sut/pv {:unit/base-pv 10 :unit/pilot {:pilot/skill 5}}) 9))))

;TODO Find a platform-independent way of testing if two files are the same.
; (t/deftest test-find-sprite
;   (t/testing "Test searching for a valid sprite."
;     (t/is (= (str (sut/find-sprite {:unit/full-name "Archer ARC-2K" :unit/chassis "Archer"})) "data/images/units/mechs/Archer_2K.png"))
;     (t/is (= (sut/find-sprite {:unit/full-name "Ahab AHB-4" :unit/chassis "Ahab"}) "data/images/units/fighter/ahab.png"))))

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
    (t/is (= (sut/get-unit "Firestarter FS9-H")
             {:unit/abilities {:attack/ht {:l 0, :l* false, :m 0, :m* false, :s 1, :s* false, :ability/output "HT1/-/-"}, :attack/rear {:l 0, :l* false, :m 0, :m* false, :s 0, :s* true, :ability/output "REAR0*/-/-"}}, :unit/armor {:toughness/current 3, :toughness/maximum 3, :toughness/unapplied 0}, :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge}, :attack/ht {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 1, :attack/s* false}, :attack/physical {:attack/damage 1, :attack/self false, :attack/type :attack/physical}, :attack/rear {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 0, :attack/m* false, :attack/s 0, :attack/s* true}, :attack/regular {:attack/e 0, :attack/e* false, :attack/l 0, :attack/l* false, :attack/m 1, :attack/m* false, :attack/s 2, :attack/s* false}}, :unit/base-pv 20, :unit/chassis "Firestarter", :unit/full-name "Firestarter FS9-H", :unit/model "FS9-H", :unit/move-modes {:move/jump 6, :move/walk 6}, :unit/mul-id 1096, :unit/overheat 0, :unit/role :role/scout, :unit/size 1, :unit/structure {:toughness/current 3, :toughness/maximum 3, :toughness/unapplied 0}, :unit/threshold -1, :unit/tmm 2, :unit/type :type/bm}))
    ;; (t/is (= (sut/get-unit "Gnome Battle Armor (Standard)") 0))
    ;; (t/is (= (sut/get-unit "Clan Foot Point (Laser)") {:role "Ambusher", :tmm 0, :left-arc "", :e* false, :movement {:f 1}, :right-arc "", :mul-id 603, :l* false, :m 1, :type "CI", :front-arc "", :abilities "AM, CAR3", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 11, :overheat 0, :chassis "Clan Foot Point", :structure 1, :full-name "Clan Foot Point (Laser)", :armor 3, :s* false, :model "(Laser)"}))
    ))
