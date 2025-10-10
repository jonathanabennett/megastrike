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
    (t/is (some #{"Hunchback IIC"} (mapv :unit/full-name (:units test-data))))
    (t/is (= (:forces test-data) [{:unit-group/camo nil,
                                   :unit-group/deployment :direction/n,
                                   :unit-group/keyword :1stsomersetstrikers,
                                   :unit-group/name "1stSomersetStrikers",
                                   :unit-group/parent 1,
                                   :unit-group/subgroups []
                                   :unit-group/player :player}
                                  {:unit-group/camo nil,
                                   :unit-group/deployment :direction/any,
                                   :unit-group/keyword :blackvision,
                                   :unit-group/name "BlackVision",
                                   :unit-group/parent 2,
                                   :unit-group/subgroups []
                                   :unit-group/player :player}]))))

(t/deftest edn-save-file-writer
  (t/testing "Test conversion of a save file"
    (let [state (sut/edn-scenario-writer (sut/setup-scenario test-scenario-path))]
      (t/is (= (:forces state)
               [{:unit-group/camo nil,
                 :unit-group/deployment :direction/n,
                 :unit-group/keyword :1stsomersetstrikers,
                 :unit-group/name "1stSomersetStrikers",
                 :unit-group/parent 1,
                 :unit-group/player :player,
                 :unit-group/subgroups []}
                {:unit-group/camo nil,
                 :unit-group/deployment :direction/any,
                 :unit-group/keyword :blackvision,
                 :unit-group/name "BlackVision",
                 :unit-group/parent 2,
                 :unit-group/player :player,
                 :unit-group/subgroups []}]))
      (t/is (= (:mapsheets state)
               ["DustballCanyon1.board" "DustballCanyon2.board"]))
      (t/is (= (map :unit/full-name (:units state))
               ["Axman AXM-2N" "Mauler MAL-1R" "Wolfhound WLF-2" "Vulture (Mad Dog) Prime" "Hunchback IIC"])))))

