(ns megastrike.heat-test
  (:require [megastrike.combat-unit :as cu]
            [megastrike.heat :as sut]
            [clojure.test :as t]))

(def no-attack (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                 {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                 :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0))
(def attacker (assoc no-attack :unit/attacked? true :unit/id "Attacked"))
(def attacker1 (assoc attacker :unit/current-heat 1 :unit/id "Attacked with heat"))
(def shutdown-unit (assoc no-attack :unit/current-heat 4 :unit/id "Shutdown"))

(t/deftest end-phase-heat
  (t/testing "Didn't attack"
    (t/is (= (:unit/current-heat (sut/end-phase-heat no-attack false)) 0) "Units that didn't attack should have zero heat")
    (t/is (= (:unit/current-heat (sut/end-phase-heat (assoc no-attack :heat/current-heat 3) false)) 0) "Even if they currently have heat, they should finish with zero."))
  (t/testing "Attacked"
    (t/is (= (:unit/current-heat (sut/end-phase-heat attacker false)) 0) "Attacked with no heat")
    (t/is (= (:unit/current-heat (sut/end-phase-heat attacker1 false)) 1) "Attacked with 1 heat")
    (t/is (= (:unit/current-heat (sut/end-phase-heat attacker true)) 0) "Attacked from water")
    (t/is (= (:unit/current-heat (sut/end-phase-heat attacker1 true)) 0) "Attacked with 1 heat from water"))
  (t/testing "Used overheat"
    (t/is (= (:unit/current-heat (sut/end-phase-heat (assoc attacker :unit/overheat-used 1) false)) 1))
    (t/is (= (:unit/current-heat (sut/end-phase-heat (assoc attacker :unit/overheat-used 1) true)) 0)))
  (t/testing "Engine Damage"
    (t/is (= (:unit/current-heat (sut/end-phase-heat (assoc-in no-attack [:unit/criticals :crits/taken] [:crits/engine]) false)) 1)))
  (t/testing "Took heat damage"
    (t/is (= (:unit/current-heat (sut/end-phase-heat (assoc attacker :unit/unapplied-heat 2) false)) 2)))
  (t/testing "Shutdown units restart"
    (t/is (= (:unit/current-heat (sut/end-phase-heat shutdown-unit false)) 0))))
