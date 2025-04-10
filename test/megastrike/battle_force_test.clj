(ns megastrike.battle-force-test
  (:require
   [clojure.test :as t]
   [megastrike.battle-force :as sut]))

(t/deftest create-battle-force
  (t/testing "Valid Battle Force Constructs"
    (t/is (= (sut/->battle-force "Test" "n" "camo" 1 :player)
             {:unit-group/keyword :test :unit-group/name "Test" :unit-group/deployment :direction/n
              :unit-group/camo "camo" :unit-group/parent 1 :unit-group/player :player}))))

