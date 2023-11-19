(ns megastrike.game-test
  (:require [megastrike.game :as sut]
            [clojure.test :as t]))

(t/deftest test-forces
  (sut/new-game)
  (t/testing "Adding forces"
    (sut/add-force! {:name "AFFS" :color "Gold" :deployment "N"})
    (sut/add-force! {:name "DCMS" :color "Red" :deployment "S"})
    (t/is (= (sut/get-forces) {"AFFS" {:name "AFFS" :color "Gold" :deployment "N"}
                               "DCMS" {:name "DCMS" :color "Red" :deployment "S"}}))
    (t/is (= (sut/get-force "AFFS") {:name "AFFS" :color "Gold" :deployment "N"}))
    (t/is (= (sut/get-force "DCMS") {:name "DCMS" :color "Red" :deployment "S"}))
    (t/is (= (sut/get-force "Not Here") nil)))
  (t/testing "Removing forces"
    (sut/del-force! "AFFS")
    (t/is (= (sut/get-forces) {"DCMS" {:name "DCMS" :color "Red" :deployment "S"}})))
  )

(t/deftest test-units
  (sut/new-game) ;; Ensure a blank slate
  (t/testing "Adding Units"
    (sut/add-unit! {:full-name "Archer ARC-1S" :force "AFFS" :deployment "N"})
    (sut/add-unit! {:full-name "Locust LCT-1V" :color "DCMS" :deployment "S"})
    (t/is (= (sut/get-units) {"Archer ARC-1S" {:full-name "Archer ARC-1S" :force "AFFS" :deployment "N"}
                               "Locust LCT-1V" {:full-name "Locust LCT-1V" :color "DCMS" :deployment "S"}}))
    (t/is (= (sut/get-unit "Archer ARC-1S") {:full-name "Archer ARC-1S" :force "AFFS" :deployment "N"}))
    (t/is (= (sut/get-unit "Locust LCT-1V") {:full-name "Locust LCT-1V" :color "DCMS" :deployment "S"}))
    (sut/add-unit! {:full-name "Archer ARC-1S" :force "DCMS" :deployment "S"})
    (t/is (= (sut/get-unit "Archer ARC-1S #2") {:full-name "Archer ARC-1S" :force "DCMS" :deployment "S"}))
    (t/is (= (sut/get-unit "Not Here") nil)))
  (t/testing "Removing forces"
    (sut/del-unit! "Archer ARC-1S")
    (t/is (= (sut/get-units) {"DCMS" {:name "DCMS" :color "Red" :deployment "S"}})))
  )
