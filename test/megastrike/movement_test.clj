(ns megastrike.movement-test
  (:require
   [clojure.test :as t]
   [clojure.walk :as walk]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.movement :as sut]))

(defn normalize-for-testing
  "Java File paths are tested elsewhere, we do not need to check them here so we are simply nilling them out."
  [data]
  (walk/postwalk
   (fn [x]
     (cond
       ;; Convert File objects to string paths
       (instance? java.io.File x) nil
       ;; Handle other problematic types here if needed
       :else x))
   data))

(def board (board/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board"))
(def attacker1 (normalize-for-testing
                (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                  {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                  :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0)))

(t/deftest test-change-facing
  (t/testing "Testing each facing."
    (t/is (= (:unit/facing (sut/change-facing attacker1 :direction/se)) :direction/se))))

(t/deftest test-print-movement
  (t/testing "Basic movement."
    (t/is (= (sut/print-movement attacker1) "6"))
    (t/is (= (sut/print-movement {:role "Test Jumper" :unit/move-modes {:move/jump 4 :move/walk 4} :unit/current-heat 0 :unit/criticals {:crits/taken [] :crits/unapplied []}}) "4j/4"))
    (t/is (= (sut/print-movement {:role "Test Strong Jumper" :unit/move-modes {:move/jump 6 :move/walk 4} :unit/current-heat 0 :unit/criticals {:crits/taken [] :crits/unapplied []}}) "6j/4"))
    (t/is (= (sut/print-movement {:role "Test Weak Jumper" :unit/move-modes {:move/jump 2 :move/walk 4} :unit/current-heat 0 :unit/criticals {:crits/taken [] :crits/unapplied []}}) "2j/4")))
  (t/testing "Damaged movement."
    (t/is (= (sut/print-movement {:role "1 MV hit", :e* false, :unit/move-modes {:move/walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :unit/criticals {:crits/taken [:crits/mv] :crits/unapplied []} :unit/current-heat 0}) "2"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :unit/move-modes {:move/walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :unit/criticals {:crits/taken [:crits/mv :crits/fire-control] :crits/unapplied []} :unit/current-heat 0}) "2"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :unit/move-modes {:move/walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :unit/criticals {:crits/taken [:crits/mv :crits/mv :crits/fire-control] :crits/unapplied []} :unit/current-heat 0}) "1"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :unit/move-modes {:move/walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :unit/criticals {:crits/taken [:crits/mv :crits/mv :crits/mv :crits/fire-control] :crits/unapplied []} :unit/current-heat 0}) "0"))
    (t/is (= (sut/print-movement {:role "Missile Boat", :tmm 1, :e* false, :unit/move-modes {:move/walk 4}, :mul-id 73, :l* false, :m 2, :type "BM", :abilities "IF2", :e 0, :s 2, :threshold -1, :l 2, :size 3, :m* false, :point-value 34, :overheat 2, :chassis "Archer", :structure 6, :full-name "Archer ARC-2K", :armor 6, :s* false, :model "ARC-2K" :unit/criticals {:crits/taken [:crits/fire-control] :crits/unapplied []} :unit/current-heat 0}) "4"))))

(t/deftest test-find-path
  (t/testing "Test finding a path"
    (t/is (= (sut/find-path attacker1 {:hex/p 6 :hex/q -2 :hex/r -4} board)
             [{:elevation 0, :palette "grass", :terrain "ground_fluff:1:2", :hex/p 1, :hex/q 1, :hex/r -2}
              {:elevation 0, :palette "grass", :terrain "ground_fluff:1:1", :hex/p 2, :hex/q 0, :hex/r -2}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 3, :hex/q 0, :hex/r -3}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 4, :hex/q -1, :hex/r -3}
              {:elevation 0,
               :palette "grass",
               :terrain "ground_fluff:1:1;water:1",
               :hex/p 5,
               :hex/q -1,
               :hex/r -4}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 6, :hex/q -2, :hex/r -4}]))))

(t/deftest test-set-path
  (t/testing "Finding a path within range"
    (t/is (= (sut/set-path attacker1 {:hex/p 6 :hex/q -2 :hex/r -4} board)
             {:move/default :move/walk,
              :move/selected :move/walk,
              :unit/abilities {:ene {:ability/output "ENE"},
                               :attack/rear {:l 0,
                                             :l* false,
                                             :m 1,
                                             :m* false,
                                             :s 1,
                                             :s* false,
                                             :ability/output "REAR1/1/-"}},
              :unit/acted? false,
              :unit/armor {:toughness/current 4, :toughness/maximum 4, :toughness/unapplied 0},
              :unit/attacks {:attack/charge {:attack/damage 0, :attack/self true, :attack/type :attack/charge},
                             :attack/physical {:attack/damage 1,
                                               :attack/self false,
                                               :attack/type :attack/physical},
                             :attack/rear {:attack/e 0,
                                           :attack/e* false,
                                           :attack/l 0,
                                           :attack/l* false,
                                           :attack/m 1,
                                           :attack/m* false,
                                           :attack/s 1,
                                           :attack/s* false},
                             :attack/regular {:attack/e 0,
                                              :attack/e* false,
                                              :attack/l 1,
                                              :attack/l* false,
                                              :attack/m 3,
                                              :attack/m* false,
                                              :attack/s 3,
                                              :attack/s* false}},
              :unit/base-pv 28,
              :unit/battle-force :1stsomersetstrikers,
              :unit/chassis "Wolfhound",
              :unit/criticals {:crits/taken [], :crits/unapplied []},
              :unit/current-heat 0,
              :unit/facing :direction/n,
              :unit/full-name "Wolfhound WLF-2",
              :unit/id "Wolfhound WLF-2",
              :unit/location {:hex/p 1, :hex/q 1, :hex/r -2},
              :unit/model "WLF-2",
              :unit/move-modes {:move/walk 6},
              :unit/mul-id 3563,
              :unit/overheat 0,
              :unit/overheat-used 0,
              :unit/path [{:elevation 0,
                           :palette "grass",
                           :terrain "ground_fluff:1:2",
                           :hex/p 1,
                           :hex/q 1,
                           :hex/r -2}
                          {:elevation 0,
                           :palette "grass",
                           :terrain "ground_fluff:1:1",
                           :hex/p 2,
                           :hex/q 0,
                           :hex/r -2}
                          {:elevation 0, :palette "grass", :terrain "", :hex/p 3, :hex/q 0, :hex/r -3}
                          {:elevation 0, :palette "grass", :terrain "", :hex/p 4, :hex/q -1, :hex/r -3}
                          {:elevation 0,
                           :palette "grass",
                           :terrain "ground_fluff:1:1;water:1",
                           :hex/p 5,
                           :hex/q -1,
                           :hex/r -4}
                          {:elevation 0, :palette "grass", :terrain "", :hex/p 6, :hex/q -2, :hex/r -4}],
              :unit/pilot {:pilot/full-name "Lieutenant Ciro Ramirez", :pilot/kills 0, :pilot/skill 4},
              :unit/role :role/striker,
              :unit/size 1,
              :unit/sprite nil
              :unit/structure {:toughness/current 4, :toughness/maximum 4, :toughness/unapplied 0},
              :unit/threshold -1,
              :unit/tmm 2,
              :unit/type :type/bm}))))

; (t/deftest test-move-cost
;   (t/testing "Test returning a movement cost"
;     (let [moved (assoc attacker1 :path (sut/set-path attacker1 {:hex/p 15 :hex/q -5, :hex/r -10} board))]
;       (t/is (= (sut/move-cost moved) 6)))))

; (t/deftest test-can-move?
;   (t/testing "Test returning a movement cost"
;     (let [moved (assoc attacker1 :path (sut/find-path attacker1 {:hex/p 15, :hex/q -5, :hex/r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} board))]
;       (t/is (= (sut/can-move? moved board)
;                {:role "Striker",
;                 :path [{:hex/p 2, :hex/q 0, :hex/r -2, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
;                        {:hex/p 3, :hex/q 0, :hex/r -3, :elevation 0, :terrain "", :palette "grass"}
;                        {:hex/p 4, :hex/q -1, :hex/r -3, :elevation 0, :terrain "", :palette "grass"}
;                        {:hex/p 5, :hex/q -1, :hex/r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
;                        {:hex/p 6, :hex/q -1, :hex/r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
;                        {:hex/p 7, :hex/q -2, :hex/r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
;                        {:hex/p 8, :hex/q -2, :hex/r -6, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
;                        {:hex/p 9, :hex/q -3, :hex/r -6, :elevation 0, :terrain "", :palette "grass"}
;                        {:hex/p 10, :hex/q -3, :hex/r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
;                        {:hex/p 11, :hex/q -3, :hex/r -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
;                        {:hex/p 12, :hex/q -4, :hex/r -8, :elevation 0, :terrain "", :palette "grass"}
;                        {:hex/p 13, :hex/q -5, :hex/r -8, :elevation 0, :terrain "", :palette "grass"}
;                        {:hex/p 14, :hex/q -5, :hex/r -9, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
;                        {:hex/p 15, :hex/q -5, :hex/r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}],
;                 :tmm 2, :hex/q 1, :left-arc "", :e* false, :movement {:walk 6},
;                 :hex/r -2, :right-arc "", :force :1stsomersetstrikers, :changes {},
;                 :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
;                 :mul-id 3563, :l* false, :m 3, :type "BM", :front-arc "",
;                 :current-structure 3, :abilities "ENE, REAR1/1/-", :acted nil,
;                 :e 0, :s 3, :threshold -1, :l 1, :size 1, :m* false,
;                 :rear-arc "", :point-value 28, :overheat 0, :chassis "Wolfhound",
;                 :structure 3, :crits [], :id "Wolfhound WLF-2",
;                 :full-name "Wolfhound WLF-2", :armor 4, :current-heat 0,
;                 :current-armor 4, :s* false, :hex/p 1, :movement-mode :walk,
;                 :direction :s, :model "WLF-2"})))))
