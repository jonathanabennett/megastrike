(ns megastrike.movement-test
  (:require [clojure.test :as t]
            [megastrike.board :as board]
            [megastrike.combat-unit :as cu]
            [megastrike.movement :as sut]))

(def board (board/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board")) 
(def attacker1 (cu/create-element (cu/get-unit "Wolfhound WLF-2") {:id "Wolfhound WLF-2" :path [] :p 1 :q 1 :r -2 :force :1stsomersetstrikers :pilot {:name " Lieutenant Ciro Ramirez", :skill 4} :acted nil :crits [] :current-structure 3 :current-heat 0 :current-armor 4 :movement-mode :walk :direction :s}))

(t/deftest test-find-path
  (t/testing "Test finding a path"
    (t/is (= (sut/find-path attacker1 {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} board) 
             [{:p 2, :q 0, :r -2, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"} {:p 3, :q 0, :r -3, :elevation 0, :terrain "", :palette "grass"} {:p 4, :q -1, :r -3, :elevation 0, :terrain "", :palette "grass"} {:p 5, :q -1, :r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"} {:p 6, :q -1, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} {:p 7, :q -2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} {:p 8, :q -2, :r -6, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"} {:p 9, :q -3, :r -6, :elevation 0, :terrain "", :palette "grass"} {:p 10, :q -3, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"} {:p 11, :q -3, :r -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"} {:p 12, :q -4, :r -8, :elevation 0, :terrain "", :palette "grass"} {:p 13, :q -5, :r -8, :elevation 0, :terrain "", :palette "grass"} {:p 14, :q -5, :r -9, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}]))))

(t/deftest test-move-cost
  (t/testing "Test returning a movement cost"
    (let [moved (assoc attacker1 :path (sut/find-path attacker1 {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} board))]
      (t/is (= (sut/move-costs moved board) [1 1 1 2 1 1 1 1 1 1 1 1 1 1])))))

(t/deftest test-can-move?
  (t/testing "Test returning a movement cost"
    (let [moved (assoc attacker1 :path (sut/find-path attacker1 {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} board))]
      (t/is (= (sut/can-move? moved board) 
               {:role "Striker", 
                :path [{:p 2, :q 0, :r -2, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                       {:p 3, :q 0, :r -3, :elevation 0, :terrain "", :palette "grass"}
                       {:p 4, :q -1, :r -3, :elevation 0, :terrain "", :palette "grass"}
                       {:p 5, :q -1, :r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                       {:p 6, :q -1, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                       {:p 7, :q -2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                       {:p 8, :q -2, :r -6, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
                       {:p 9, :q -3, :r -6, :elevation 0, :terrain "", :palette "grass"}
                       {:p 10, :q -3, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                       {:p 11, :q -3, :r -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                       {:p 12, :q -4, :r -8, :elevation 0, :terrain "", :palette "grass"}
                       {:p 13, :q -5, :r -8, :elevation 0, :terrain "", :palette "grass"}
                       {:p 14, :q -5, :r -9, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                       {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}], 
                :tmm 2, :q 1, :left-arc "", :e* false, :movement {:walk 6}, 
                :r -2, :right-arc "", :force :1stsomersetstrikers, 
                :pilot {:name " Lieutenant Ciro Ramirez", :skill 4},
                :mul-id 3563, :l* false, :m 3, :type "BM", :front-arc "", 
                :current-structure 3, :abilities "ENE, REAR1/1/-", :acted nil, 
                :e 0, :s 3, :threshold -1, :l 1, :size 1, :m* false, 
                :rear-arc "", :point-value 28, :overheat 0, :chassis "Wolfhound", 
                :structure 3, :crits [], :id "Wolfhound WLF-2", 
                :full-name "Wolfhound WLF-2", :armor 4, :current-heat 0, 
                :current-armor 4, :s* false, :p 1, :movement-mode :walk, 
                :direction :s, :model "WLF-2"})))))
