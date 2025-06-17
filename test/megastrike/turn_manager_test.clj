(ns megastrike.turn-manager-test
  (:require
   [clojure.test :as t]
   [clojure.walk :as walk]
   [megastrike.board :as board]
   [megastrike.combat-unit :as cu]
   [megastrike.turn-manager :as sut]))

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
(def simple-game-state
  {:units {"Wolfhound WLF-2"
           (normalize-for-testing (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                                    {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                                    :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0))
           "Wolfhound WLF-2 #2"
           (normalize-for-testing (assoc (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                                           {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                                           :direction/n {:hex/p 2 :hex/q 1 :hex/r -3} :1stsomersetstrikers 0) :unit/id "Wolfhound WLF-2 #2"))}
   :round-report ""
   :board board})

(t/deftest test-unit-updates
  (t/testing "Valid attack"
    (t/is (= (sut/unit-updates simple-game-state [[[:units "Wolfhound WLF-2" :unit/acted?] true]
                                                  [[:units "Wolfhound WLF-2" :unit/attacked?] true]
                                                  [[:units "Wolfhound WLF-2 #2" :unit/armor :toughness/unapplied] 3]
                                                  [[:units "Wolfhound WLF-2 #2" :unit/structure :toughness/unapplied] 0]
                                                  [[:units "Wolfhound WLF-2 #2" :unit/criticals :crits/unapplied] []]])
             {:units {"Wolfhound WLF-2" {:move/default :move/walk,
                                         :move/selected nil,
                                         :unit/abilities {:ene {:ability/output "ENE"},
                                                          :attack/rear {:l 0,
                                                                        :l* false,
                                                                        :m 1,
                                                                        :m* false,
                                                                        :s 1,
                                                                        :s* false,
                                                                        :ability/output "REAR1/1/-"}},
                                         :unit/acted? true,
                                         :unit/armor {:toughness/current 4,
                                                      :toughness/maximum 4,
                                                      :toughness/unapplied 0},
                                         :unit/attacked? true,
                                         :unit/attacks {:attack/charge {:attack/damage 0,
                                                                        :attack/self true,
                                                                        :attack/type :attack/charge},
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
                                         :unit/path [],
                                         :unit/pilot {:pilot/full-name "Lieutenant Ciro Ramirez",
                                                      :pilot/kills 0,
                                                      :pilot/skill 4},
                                         :unit/role :role/striker,
                                         :unit/size 1,
                                         :unit/sprite nil,
                                         :unit/structure {:toughness/current 4,
                                                          :toughness/maximum 4,
                                                          :toughness/unapplied 0},
                                         :unit/threshold -1,
                                         :unit/tmm 2,
                                         :unit/type :type/bm},
                      "Wolfhound WLF-2 #2" {:move/default :move/walk,
                                            :move/selected nil,
                                            :unit/abilities {:ene {:ability/output "ENE"},
                                                             :attack/rear {:l 0,
                                                                           :l* false,
                                                                           :m 1,
                                                                           :m* false,
                                                                           :s 1,
                                                                           :s* false,
                                                                           :ability/output "REAR1/1/-"}},
                                            :unit/acted? false,
                                            :unit/armor {:toughness/current 4,
                                                         :toughness/maximum 4,
                                                         :toughness/unapplied 3},
                                            :unit/attacks {:attack/charge {:attack/damage 0,
                                                                           :attack/self true,
                                                                           :attack/type :attack/charge},
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
                                            :unit/id "Wolfhound WLF-2 #2",
                                            :unit/location {:hex/p 2, :hex/q 1, :hex/r -3},
                                            :unit/model "WLF-2",
                                            :unit/move-modes {:move/walk 6},
                                            :unit/mul-id 3563,
                                            :unit/overheat 0,
                                            :unit/overheat-used 0,
                                            :unit/path [],
                                            :unit/pilot {:pilot/full-name "Lieutenant Ciro Ramirez",
                                                         :pilot/kills 0,
                                                         :pilot/skill 4},
                                            :unit/role :role/striker,
                                            :unit/size 1,
                                            :unit/sprite nil,
                                            :unit/structure {:toughness/current 4,
                                                             :toughness/maximum 4,
                                                             :toughness/unapplied 0},
                                            :unit/threshold -1,
                                            :unit/tmm 2,
                                            :unit/type :type/bm}}}))))

(t/deftest parse-attack-data
  (t/testing "Successful attack"
    (t/is (= (sut/parse-attack-data {:combat-result/armor-damage 3,
                                     :combat-result/attack :attack/regular,
                                     :combat-result/attacker "Wolfhound WLF-2",
                                     :combat-result/changes [[[:units "Wolfhound WLF-2" :unit/acted?] true]
                                                             [[:units "Wolfhound WLF-2" :unit/attacked?] true]
                                                             [[:units "Wolfhound WLF-2 #2" :unit/armor :toughness/unapplied] 3]
                                                             [[:units "Wolfhound WLF-2 #2" :unit/structure :toughness/unapplied] 0]
                                                             [[:units "Wolfhound WLF-2 #2" :unit/criticals :crits/unapplied] []]],
                                     :combat-result/crits [nil nil],
                                     :combat-result/damage 3,
                                     :combat-result/penetration 0,
                                     :combat-result/roll 8,
                                     :combat-result/target "Wolfhound WLF-2 #2",
                                     :combat-result/target-number 6})
             "Wolfhound WLF-2 attacks Wolfhound WLF-2 #2. Using a regular attack. Needs a 6.\nRolled a 8\nAttack hits for 3 damage.\n3 damage to armor.\n\n\n")))
  (t/testing "Unsuccessful attack")
  (t/is (= (sut/parse-attack-data {:combat-result/armor-damage 3,
                                   :combat-result/attack :attack/regular,
                                   :combat-result/attacker "Wolfhound WLF-2",
                                   :combat-result/changes [[[:units "Wolfhound WLF-2" :unit/acted?] true]
                                                           [[:units "Wolfhound WLF-2" :unit/attacked?] true]
                                                           [[:units "Wolfhound WLF-2 #2" :unit/armor :toughness/unapplied] 3]
                                                           [[:units "Wolfhound WLF-2 #2" :unit/structure :toughness/unapplied] 0]
                                                           [[:units "Wolfhound WLF-2 #2" :unit/criticals :crits/unapplied] []]],
                                   :combat-result/crits [nil nil],
                                   :combat-result/damage 3,
                                   :combat-result/penetration 0,
                                   :combat-result/roll 4,
                                   :combat-result/target "Wolfhound WLF-2 #2",
                                   :combat-result/target-number 6})
           "Wolfhound WLF-2 attacks Wolfhound WLF-2 #2. Using a regular attack. Needs a 6.\nRolled a 4\nAttack misses.\n\n\n\n")))

(t/deftest test-make-attack
  (t/testing "Roll regular attack."
    (t/is (= (:round-report (sut/make-attack simple-game-state (first (vals (attacks/->targeting (get-in simple-game-state [:units "Wolfhound WLF-2"])
                                                                                                 (get-in simple-game-state [:units "Wolfhound WLF-2 #2"])
                                                                                                 (:board simple-game-state)
                                                                                                 (hex/create-layout)
                                                                                                 :attack/regular))))) ""))))

