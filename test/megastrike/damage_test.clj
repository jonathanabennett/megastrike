(ns megastrike.damage-test
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as t]
   [megastrike.combat-unit :as cu]
   [megastrike.damage :as sut]))

(def attacker1 (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                                 {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                                 :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0))

(def armor-damaged (-> attacker1
                       (sut/take-damage 2 false)
                       (sut/apply-damage)))

(def armor-destroyed (-> attacker1
                         (sut/take-damage 4 false)
                         (sut/apply-damage)))

(def internal-damage (-> attacker1
                         (sut/take-damage 5 false)
                         (sut/apply-damage)))

(def two-attacks (-> attacker1
                     (sut/take-damage 2 false)
                     (sut/take-damage 2 false)
                     (sut/apply-damage)))

(t/deftest remaining-armor
  (t/testing "valid responses"
    (t/is (= (sut/remaining-armor attacker1) 4))
    (t/is (= (sut/remaining-armor armor-damaged) 2))
    (t/is (= (sut/remaining-armor armor-destroyed) 0))
    (t/is (= (sut/remaining-armor internal-damage) 0))
    (t/is (= (sut/remaining-armor two-attacks) 0))))

(t/deftest remaining-structure
  (t/testing "valid responses"
    (t/is (= (sut/remaining-structure attacker1) 4))
    (t/is (= (sut/remaining-structure attacker1) 4))
    (t/is (= (sut/remaining-structure armor-destroyed) 4))
    (t/is (= (sut/remaining-structure internal-damage) 3))
    (t/is (= (sut/remaining-structure two-attacks) 4))))
