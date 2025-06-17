(ns megastrike.scenario-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :as t]
   [megastrike.scenario :as sut]))

(def scenario-folder (io/file "data/scenarios"))
(def test-scenario-path "data/scenarios/1stSomersetStrikers/1-ClashInTheCanyon.mms")
(def test-scenarios (filter #(.isFile %) (file-seq scenario-folder)))
(def test-data (sut/parse-scenario-file test-scenario-path))

(t/deftest parse-scenario-file
  (t/testing "Tests all scenario files"
    (run! #(t/is (not (nil? (sut/parse-scenario-file %)))) test-scenarios)
    (t/is (= (count (:units test-data)) 5))
    (t/is (= (:forces test-data) {:1stsomersetstrikers {:unit-group/camo nil,
                                                        :unit-group/deployment :direction/n,
                                                        :unit-group/keyword :1stsomersetstrikers,
                                                        :unit-group/name "1stSomersetStrikers",
                                                        :unit-group/parent 1,
                                                        :unit-group/player :player},
                                  :blackvision {:unit-group/camo nil,
                                                :unit-group/deployment :direction/any,
                                                :unit-group/keyword :blackvision,
                                                :unit-group/name "BlackVision",
                                                :unit-group/parent 2,
                                                :unit-group/player :player}}))))
