(ns megastrike.combat-unit-test
  (:require [megastrike.combat-unit :as sut]
            [clojure-csv.core :as csv]
            [clojure.test :as t]))

(t/deftest test-move-parser
  (t/testing "Valid Mv Strings"
    (t/is (= (sut/parse-movement "6\\\"t") {:t 3}))
    (t/is (= (sut/parse-movement "16\\\"/12\\\"j") {:walk 8 :jump 6}))))

(t/deftest test-mul-parser
  (let [bm-test "73	Archer	ARC-2K	Missile Boat	BM	3	8\"	1	6	6	-1	2	FALSE	2	FALSE	2	FALSE	0	FALSE	2	34	IF2"
        cv-test "4879	Puma Assault Tank	PAT-001	None	CV	4	6\"t	1	6	5	-1	4	FALSE	5	FALSE	3	FALSE	0	FALSE	0	37	IF2, LRM1/2/2, REAR1/-/-, SRCH, TUR(1/1/1)"
        sv-test "3684	Air Car		None	SV	2	22\"h	4	1	2	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	6	BAR, EE, ENE"
        pm-test "510	Centaur	2	Scout	PM	1	12\"	2	1	1	-1	1	FALSE	0	TRUE	0	FALSE	0	FALSE	0	10					"
        ci-test "-1	Motorized Sub Platoon	(Laser SCUBA)	Undetermined	CI	1	4\"s	0	1	1	-1	1	FALSE	1	FALSE	0	FALSE	0	FALSE	0	8	AM, CAR4, UMU				"
        im-test "4545	Lumberjack	LM1/A	Ambusher	IM	3	6\"	1	1	5	-1	0	FALSE	0	FALSE	0	FALSE	0	FALSE	0	7	BAR, BFC, CT8, EE, ENE, MEL, SAW				"
        ba-test "952	Elemental Battle Armor	(Headhunter)(Sqd4)	Ambusher	BA	1	6\"j	1	1	2	-1	1	FALSE	0	FALSE	0	FALSE	0	FALSE	0	15	AM, CAR4, MEC, RCN, RSD1				"]
    (t/testing "Sample MUL rows"
      (t/is (= (sut/parse-row (first (csv/parse-csv bm-test :delimiter \tab))) {:role "Missile Boat", :tmm 1, :e* false, :movement {}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv cv-test :delimiter \tab))) {:role "None", :tmm 1, :e* false, :movement {}, :mul-id 4879, :l* false, :m 5, :type "CV", :abilities "IF2, LRM1/2/2, REAR1/-/-, SRCH, TUR(1/1/1)", :e 0, :s 4, :threshold -1, :l 3, :size 4, :m* false, :point-value 37, :overheat 0, :chassis "Puma Assault Tank", :structure 5, :full-name "Puma Assault Tank PAT-001", :armor 6, :s* false, :model "PAT-001"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv sv-test :delimiter \tab))) {:role "None", :tmm 4, :e* false, :movement {}, :mul-id 3684, :l* false, :m 0, :type "SV", :abilities "BAR, EE, ENE", :e 0, :s 0, :threshold -1, :l 0, :size 2, :m* false, :point-value 6, :overheat 0, :chassis "Air Car", :structure 2, :full-name "Air Car ", :armor 1, :s* false, :model ""}))
      (t/is (= (sut/parse-row (first (csv/parse-csv pm-test :delimiter \tab))) {:role "Scout", :tmm 2, :left-arc "", :e* false, :movement {}, :right-arc "", :mul-id 510, :l* false, :m 0, :type "PM", :front-arc "", :abilities "", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* true, :rear-arc "", :point-value 10, :overheat 0, :chassis "Centaur", :structure 1, :full-name "Centaur 2", :armor 1, :s* false, :model "2"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ci-test :delimiter \tab))) {:role "Undetermined", :tmm 0, :left-arc "", :e* false, :movement {}, :right-arc "", :mul-id -1, :l* false, :m 1, :type "CI", :front-arc "", :abilities "AM, CAR4, UMU", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 8, :overheat 0, :chassis "Motorized Sub Platoon", :structure 1, :full-name "Motorized Sub Platoon (Laser SCUBA)", :armor 1, :s* false, :model "(Laser SCUBA)"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv im-test :delimiter \tab))) {:role "Ambusher", :tmm 1, :left-arc "", :e* false, :movement {}, :right-arc "", :mul-id 4545, :l* false, :m 0, :type "IM", :front-arc "", :abilities "BAR, BFC, CT8, EE, ENE, MEL, SAW", :e 0, :s 0, :threshold -1, :l 0, :size 3, :m* false, :rear-arc "", :point-value 7, :overheat 0, :chassis "Lumberjack", :structure 5, :full-name "Lumberjack LM1/A", :armor 1, :s* false, :model "LM1/A"}))
      (t/is (= (sut/parse-row (first (csv/parse-csv ba-test :delimiter \tab))) {:role "Ambusher", :tmm 1, :left-arc "", :e* false, :movement {}, :right-arc "", :mul-id 952, :l* false, :m 0, :type "BA", :front-arc "", :abilities "AM, CAR4, MEC, RCN, RSD1", :e 0, :s 1, :threshold -1, :l 0, :size 1, :m* false, :rear-arc "", :point-value 15, :overheat 0, :chassis "Elemental Battle Armor", :structure 2, :full-name "Elemental Battle Armor (Headhunter)(Sqd4)", :armor 1, :s* false, :model "(Headhunter)(Sqd4)"})))))

(t/deftest test-filters
  (t/testing "Filter by name"
    (t/is (= (:point-value (sut/get-unit (sut/filter-units sut/mul :full-name "Archer ARC-2K" =))) 34)))
  (t/testing "Empty filters return unaltered lists."
    (t/is (= (sut/filter-units sut/mul) sut/mul))))

(t/deftest test-create-element
  (t/testing "Verify new keys merged."
    (t/is (contains? (sut/create-element (sut/get-unit (sut/filter-units sut/mul :full-name "Archer ARC-2K" =))
                                        {:pilot {:name "Bobby McSkillface" :skill 4}}) :pilot))))

(t/deftest test-pv-mod-calculation
  (t/testing "When the skill is 4, pv-mod is 0"
    (t/is (= (:pv-mod (sut/pv-mod {:point-value 10 :pilot {:skill 4}})) 0))
    (t/is (= (:pv-mod (sut/pv-mod {:point-value 10 :pilot {:skill 3}})) 2))
    (t/is (= (:pv-mod (sut/pv-mod {:point-value 10 :pilot {:skill 5}})) -1)))
  (t/testing "Check PV Mod calculation."
    (t/is (= (sut/pv (sut/pv-mod {:point-value 10 :pilot {:skill 4}})) 10))
    (t/is (= (sut/pv (sut/pv-mod {:point-value 10 :pilot {:skill 3}})) 12))
    (t/is (= (sut/pv (sut/pv-mod {:point-value 10 :pilot {:skill 5}})) 9))))
