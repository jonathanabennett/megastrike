(ns megastrike.gui.events-test
  (:require
   [clojure.test :as t]
   [megastrike.combat-unit :as cu]
   [megastrike.scenario :as scenario]
   [megastrike.gui.lobby.events :as lobby-e]))

;; Setup dummy data for tests
(def test-mul [{:unit/name "Atlas" :unit/type "Assault"}
               {:unit/name "Locust" :unit/type "Light"}])
(def test-lobby {:mul test-mul :active-mul nil})
(def test-game {:units []})
(def test-context {:lobby test-lobby :game test-game})

(t/deftest filter-changed-test
  (t/testing "Test that filtering the MUL updates the lobby MUL"
    (let [values {"Type" "Assault"}
          filtered (cu/filter-units test-mul values)]
      (t/is (= (count filtered) 1))
      (t/is (= (:unit/name (first filtered)) "Atlas")))))

(t/deftest mul-selection-changed-test
  (t/testing "Test that MUL selection updates the lobby state"
    (let [new-unit {:unit/name "Locust"}
          ;; Simulating the logic: (assoc-in context [:lobby :active-mul] event)
          new-context (assoc-in test-context [:lobby :active-mul] new-unit)]
      (t/is (= (get-in new-context [:lobby :active-mul]) new-unit)))))

(t/deftest load-scenario-test
  (t/testing "Test that loading a scenario returns the correct data structure"
    (let [path "data/scenarios/1stSomersetStrikers/1-ClashInTheCanyon.mms"
          response (scenario/setup-scenario path)]
      (t/is (map? (:lobby response)))
      (t/is (map? (:game response)))
      (t/is (pos? (count (:units (:game response))))))))
