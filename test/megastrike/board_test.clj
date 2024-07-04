(ns megastrike.board-test
  (:require [megastrike.board :as sut]
            [clojure.test :as t]))

(t/deftest test-create-tile
  (t/testing "Valid tiles"
    (t/is (= (sut/create-tile 0 0 0 1 "" "Grass")
             {:p 0 :q 0 :r 0 :elevation 1 :terrain "" :palette "Grass"}))
    (t/is (= (sut/create-tile 2 2 1 "" "Grass")
             {:p 2, :q 1, :r -3, :elevation 1, :terrain "", :palette "Grass"}))))

(t/deftest test-parse-hex-line
  (t/testing "Test valid hex lines"
    (t/is (= (sut/parse-hex-line "hex 0101 0 \"ground_fluff:1:2\" \"grass\"") 
             {:p 1, :q 1, :r -2, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0501 0 \"ground_fluff:1:1;water:1\" \"grass\"") 
             {:p 5, :q -1, :r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0402 0 \"woods:1:20;ground_fluff:3:1;foliage_elev:2\" \"grass\"")
             {:p 4, :q 0, :r -4, :elevation 0, :terrain "woods:1:20;ground_fluff:3:1;foliage_elev:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1202 3 \"\" \"grass\"")
             {:p 12, :q -4, :r -8, :elevation 3, :terrain "", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0808 0 \"bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3\" \"grass\"")
             {:p 8, :q 4, :r -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0312 0 \"road:1:9;ground_fluff:1:1\" \"grass\"")
             {:p 3, :q 11, :r -14, :elevation 0, :terrain "road:1:9;ground_fluff:1:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1301 -1 \"\" \"fungus\"")
             {:p 13, :q -5, :r -8, :elevation -1, :terrain "", :palette "fungus"}))))

(t/deftest test-hex-line
  (t/testing "Test drawing a straight line for LOS purposes"
    (let [board (sut/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board")
          start1 {:p 4 :q 4 :r -8}
          end1 {:p 6 :q 0 :r -6}
          start2 {:p 2 :q 2 :r -4}
          end2 {:p 15 :q -4 :r -11}]
      (t/is (= (sut/hex-line start1 end1 board) 
               [{:p 4, :q 4, :r -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 5, :q 3, :r -8, :elevation 2, :terrain "", :palette "grass"}
                {:p 5, :q 2, :r -7, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 6, :q 1, :r -7, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 6, :q 0, :r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}]))
      (t/is (= (sut/hex-line start2 end2 board) 
               [{:p 2, :q 2, :r -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 3, :q 2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:p 4, :q 1, :r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 5, :q 1, :r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 6, :q 0, :r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 7, :q 0, :r -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
                {:p 8, :q -1, :r -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:p 9, :q -1, :r -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
                {:p 10, :q -2, :r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 11, :q -2, :r -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 12, :q -3, :r -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
                {:p 13, :q -3, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:p 14, :q -4, :r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 15, :q -4, :r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}]))
      (t/is (= (sut/hex-line start1 end2 board) 
               [{:p 4, :q 4, :r -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 5, :q 3, :r -8, :elevation 2, :terrain "", :palette "grass"}
                {:p 6, :q 3, :r -9, :elevation 1, :terrain "", :palette "grass"}
                {:p 7, :q 2, :r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
                {:p 8, :q 1, :r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
                {:p 9, :q 0, :r -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 10, :q 0, :r -10, :elevation 1, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:p 11, :q -1, :r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 12, :q -2, :r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 13, :q -3, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:p 14, :q -3, :r -11, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 15, :q -4, :r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"} ]))
      (t/is (= (sut/hex-line start2 end2 board)
               [{:p 2, :q 2, :r -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"} 
                {:p 3, :q 2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:p 4, :q 1, :r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 5, :q 1, :r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:p 6, :q 0, :r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 7, :q 0, :r -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
                {:p 8, :q -1, :r -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:p 9, :q -1, :r -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
                {:p 10, :q -2, :r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:p 11, :q -2, :r -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 12, :q -3, :r -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
                {:p 13, :q -3, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:p 14, :q -4, :r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:p 15, :q -4, :r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}])))))

(t/deftest test-create-board
  (t/testing "Test an empty board."
    (t/is (= (sut/nodes (sut/create-board 3 3)) 
             [{:p 1, :q 1, :r -2, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 2, :r -3, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 3, :r -4, :elevation 0, :terrain "", :palette "grass"}
              {:p 2, :q 0, :r -2, :elevation 0, :terrain "", :palette "grass"} {:p 2, :q 1, :r -3, :elevation 0, :terrain "", :palette "grass"} {:p 2, :q 2, :r -4, :elevation 0, :terrain "", :palette "grass"} {:p 3, :q 0, :r -3, :elevation 0, :terrain "", :palette "grass"} {:p 3, :q 1, :r -4, :elevation 0, :terrain "", :palette "grass"} {:p 3, :q 2, :r -5, :elevation 0, :terrain "", :palette "grass"}])))
  (t/testing "Test an example board file."
    (t/is (= (sut/nodes (sut/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board"))
             [{:p 1, :q 1, :r -2, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 0, :r -2, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 0, :r -3, :elevation 0, :terrain "", :palette "grass"}
              {:p 4, :q -1, :r -3, :elevation 0, :terrain "", :palette "grass"}
              {:p 5, :q -1, :r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 6, :q -2, :r -4, :elevation 0, :terrain "", :palette "grass"}
              {:p 7, :q -2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 8, :q -3, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 9, :q -3, :r -6, :elevation 0, :terrain "", :palette "grass"}
              {:p 10, :q -4, :r -6, :elevation 0, :terrain "", :palette "grass"}
              {:p 11, :q -4, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 12, :q -5, :r -7, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q -5, :r -8, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q -6, :r -8, :elevation 0, :terrain "", :palette "grass"}
              {:p 15, :q -6, :r -9, :elevation 0, :terrain "", :palette "grass"}
              {:p 16, :q -7, :r -9, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 2, :r -3, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 1, :r -3, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 1, :r -4, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 4, :q 0, :r -4, :elevation 0, :terrain "woods:1:20;ground_fluff:3:1;foliage_elev:2", :palette "grass"}
              {:p 5, :q 0, :r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 6, :q -1, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 7, :q -1, :r -6, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 8, :q -2, :r -6, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
              {:p 9, :q -2, :r -7, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 10, :q -3, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 11, :q -3, :r -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 12, :q -4, :r -8, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q -4, :r -9, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q -5, :r -9, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 15, :q -5, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 16, :q -6, :r -10, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 3, :r -4, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 2, :r -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 2, :r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 4, :q 1, :r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 5, :q 1, :r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 6, :q 0, :r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 7, :q 0, :r -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
              {:p 8, :q -1, :r -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:p 9, :q -1, :r -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
              {:p 10, :q -2, :r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 11, :q -2, :r -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 12, :q -3, :r -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:p 13, :q -3, :r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 14, :q -4, :r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q -4, :r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 16, :q -5, :r -11, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 4, :r -5, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 3, :r -5, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 3, :r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 4, :q 2, :r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 5, :q 2, :r -7, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 6, :q 1, :r -7, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 7, :q 1, :r -8, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:p 8, :q 0, :r -8, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 9, :q 0, :r -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 10, :q -1, :r -9, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 11, :q -1, :r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 12, :q -2, :r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 13, :q -2, :r -11, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:p 14, :q -3, :r -11, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 15, :q -3, :r -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 16, :q -4, :r -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 5, :r -6, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 4, :r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 4, :r -7, :elevation 0, :terrain "water:1", :palette "grass"}
              {:p 4, :q 3, :r -7, :elevation 2, :terrain "", :palette "grass"}
              {:p 5, :q 3, :r -8, :elevation 2, :terrain "", :palette "grass"}
              {:p 6, :q 2, :r -8, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 7, :q 2, :r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 8, :q 1, :r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 9, :q 1, :r -10, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 10, :q 0, :r -10, :elevation 1, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:p 11, :q 0, :r -11, :elevation 2, :terrain "", :palette "grass"}
              {:p 12, :q -1, :r -11, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 13, :q -1, :r -12, :elevation 1, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 14, :q -2, :r -12, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 15, :q -2, :r -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 16, :q -3, :r -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 6, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 2, :q 5, :r -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 5, :r -8, :elevation 0, :terrain "water:1", :palette "grass"}
              {:p 4, :q 4, :r -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:p 5, :q 4, :r -9, :elevation 2, :terrain "", :palette "grass"}
              {:p 6, :q 3, :r -9, :elevation 1, :terrain "", :palette "grass"}
              {:p 7, :q 3, :r -10, :elevation 0, :terrain "ground_fluff:1:4;rough:1:20", :palette "grass"}
              {:p 8, :q 2, :r -10, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 9, :q 2, :r -11, :elevation 1, :terrain "", :palette "grass"}
              {:p 10, :q 1, :r -11, :elevation 2, :terrain "", :palette "grass"}
              {:p 11, :q 1, :r -12, :elevation 3, :terrain "", :palette "grass"}
              {:p 12, :q 0, :r -12, :elevation 2, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 13, :q 0, :r -13, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 14, :q -1, :r -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 15, :q -1, :r -14, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 16, :q -2, :r -14, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 7, :r -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 2, :q 6, :r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 3, :q 6, :r -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 4, :q 5, :r -9, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 5, :q 5, :r -10, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:p 6, :q 4, :r -10, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:p 7, :q 4, :r -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 8, :q 3, :r -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 9, :q 3, :r -12, :elevation 1, :terrain "", :palette "grass"}
              {:p 10, :q 2, :r -12, :elevation 0, :terrain "road:1:20;ground_fluff:1:2", :palette "grass"}
              {:p 11, :q 2, :r -13, :elevation 2, :terrain "", :palette "grass"}
              {:p 12, :q 1, :r -13, :elevation 1, :terrain "", :palette "grass"}
              {:p 13, :q 1, :r -14, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 14, :q 0, :r -14, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 0, :r -15, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:p 16, :q -1, :r -15, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 8, :r -9, :elevation 0, :terrain "", :palette "grass"}
              {:p 2, :q 7, :r -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 3, :q 7, :r -10, :elevation 0, :terrain "ground_fluff:1:3;rough:1:20", :palette "grass"}
              {:p 4, :q 6, :r -10, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 5, :q 6, :r -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 6, :q 5, :r -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 7, :q 5, :r -12, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:p 8, :q 4, :r -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:p 9, :q 4, :r -13, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:p 10, :q 3, :r -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 11, :q 3, :r -14, :elevation 0, :terrain "road:1:36", :palette "grass"}
              {:p 12, :q 2, :r -14, :elevation 0, :terrain "road:1:34", :palette "grass"}
              {:p 13, :q 2, :r -15, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:p 14, :q 1, :r -15, :elevation 1, :terrain "", :palette "grass"}
              {:p 15, :q 1, :r -16, :elevation 0, :terrain "", :palette "grass"}
              {:p 16, :q 0, :r -16, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 9, :r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 2, :q 8, :r -10, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:p 3, :q 8, :r -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:p 4, :q 7, :r -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 5, :q 7, :r -12, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:p 6, :q 6, :r -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:2", :palette "grass"}
              {:p 7, :q 6, :r -13, :elevation 0, :terrain "water:1;bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:2", :palette "grass"}
              {:p 8, :q 5, :r -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 9, :q 5, :r -14, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 10, :q 4, :r -14, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 11, :q 4, :r -15, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 3, :r -15, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 3, :r -16, :elevation 2, :terrain "", :palette "grass"}
              {:p 14, :q 2, :r -16, :elevation 1, :terrain "", :palette "grass"}
              {:p 15, :q 2, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 16, :q 1, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 10, :r -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 2, :q 9, :r -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 9, :r -12, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 4, :q 8, :r -12, :elevation 0, :terrain "road:1:18;ground_fluff:1:3", :palette "grass"}
              {:p 5, :q 8, :r -13, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:p 6, :q 7, :r -13, :elevation 2, :terrain "", :palette "grass"}
              {:p 7, :q 7, :r -14, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 8, :q 6, :r -14, :elevation 0, :terrain "woods:1:20;ground_fluff:1:2;foliage_elev:2", :palette "grass"}
              {:p 9, :q 6, :r -15, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 10, :q 5, :r -15, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 11, :q 5, :r -16, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 4, :r -16, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 4, :r -17, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:p 14, :q 3, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 15, :q 3, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 16, :q 2, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 1, :q 11, :r -12, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 10, :r -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 10, :r -13, :elevation 0, :terrain "road:1:10;ground_fluff:1:1", :palette "grass"}
              {:p 4, :q 9, :r -13, :elevation 2, :terrain "", :palette "grass"}
              {:p 5, :q 9, :r -14, :elevation 3, :terrain "", :palette "grass"}
              {:p 6, :q 8, :r -14, :elevation 3, :terrain "", :palette "grass"}
              {:p 7, :q 8, :r -15, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 8, :q 7, :r -15, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 9, :q 7, :r -16, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 10, :q 6, :r -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 11, :q 6, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 5, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 5, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q 4, :r -18, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 4, :r -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 16, :q 3, :r -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 12, :r -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 2, :q 11, :r -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 3, :q 11, :r -14, :elevation 0, :terrain "road:1:9;ground_fluff:1:1", :palette "grass"}
              {:p 4, :q 10, :r -14, :elevation 2, :terrain "", :palette "grass"}
              {:p 5, :q 10, :r -15, :elevation 2, :terrain "", :palette "grass"}
              {:p 6, :q 9, :r -15, :elevation 2, :terrain "", :palette "grass"}
              {:p 7, :q 9, :r -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 8, :q 8, :r -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 9, :q 8, :r -17, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 10, :q 7, :r -17, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 11, :q 7, :r -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 12, :q 6, :r -18, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 13, :q 6, :r -19, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q 5, :r -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 5, :r -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 16, :q 4, :r -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 13, :r -14, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 2, :q 12, :r -14, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:p 3, :q 12, :r -15, :elevation 0, :terrain "road:1:17", :palette "grass"}
              {:p 4, :q 11, :r -15, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 5, :q 11, :r -16, :elevation 2, :terrain "", :palette "grass"}
              {:p 6, :q 10, :r -16, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 7, :q 10, :r -17, :elevation 1, :terrain "", :palette "grass"}
              {:p 8, :q 9, :r -17, :elevation 1, :terrain "", :palette "grass"}
              {:p 9, :q 9, :r -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 10, :q 8, :r -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 11, :q 8, :r -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 12, :q 7, :r -19, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 7, :r -20, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
              {:p 14, :q 6, :r -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 6, :r -21, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 16, :q 5, :r -21, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 14, :r -15, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:p 2, :q 13, :r -15, :elevation 0, :terrain "", :palette "grass"}
              {:p 3, :q 13, :r -16, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:p 4, :q 12, :r -16, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 5, :q 12, :r -17, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 6, :q 11, :r -17, :elevation 1, :terrain "", :palette "grass"}
              {:p 7, :q 11, :r -18, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 8, :q 10, :r -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 9, :q 10, :r -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 10, :q 9, :r -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 11, :q 9, :r -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 12, :q 8, :r -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 13, :q 8, :r -21, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:p 14, :q 7, :r -21, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 7, :r -22, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 16, :q 6, :r -22, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 15, :r -16, :elevation 0, :terrain "", :palette "grass"}
              {:p 2, :q 14, :r -16, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 3, :q 14, :r -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 4, :q 13, :r -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 5, :q 13, :r -18, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:p 6, :q 12, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 7, :q 12, :r -19, :elevation 0, :terrain "", :palette "grass"}
              {:p 8, :q 11, :r -19, :elevation 0, :terrain "", :palette "grass"}
              {:p 9, :q 11, :r -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:p 10, :q 10, :r -20, :elevation 0, :terrain "", :palette "grass"}
              {:p 11, :q 10, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 9, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 9, :r -22, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q 8, :r -22, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:p 15, :q 8, :r -23, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 16, :q 7, :r -23, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 16, :r -17, :elevation 0, :terrain "", :palette "grass"}
              {:p 2, :q 15, :r -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 3, :q 15, :r -18, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 4, :q 14, :r -18, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 5, :q 14, :r -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 6, :q 13, :r -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 7, :q 13, :r -20, :elevation 0, :terrain "", :palette "grass"}
              {:p 8, :q 12, :r -20, :elevation 0, :terrain "", :palette "grass"}
              {:p 9, :q 12, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 10, :q 11, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 11, :q 11, :r -22, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 10, :r -22, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 10, :r -23, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q 9, :r -23, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 9, :r -24, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:p 16, :q 8, :r -24, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 1, :q 17, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 2, :q 16, :r -18, :elevation 0, :terrain "", :palette "grass"}
              {:p 3, :q 16, :r -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 4, :q 15, :r -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 5, :q 15, :r -20, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:p 6, :q 14, :r -20, :elevation 0, :terrain "", :palette "grass"}
              {:p 7, :q 14, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 8, :q 13, :r -21, :elevation 0, :terrain "", :palette "grass"}
              {:p 9, :q 13, :r -22, :elevation 0, :terrain "", :palette "grass"}
              {:p 10, :q 12, :r -22, :elevation 0, :terrain "", :palette "grass"}
              {:p 11, :q 12, :r -23, :elevation 0, :terrain "", :palette "grass"}
              {:p 12, :q 11, :r -23, :elevation 0, :terrain "", :palette "grass"}
              {:p 13, :q 11, :r -24, :elevation 0, :terrain "", :palette "grass"}
              {:p 14, :q 10, :r -24, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 15, :q 10, :r -25, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:p 16, :q 9, :r -25, :elevation 0, :terrain "", :palette "grass"}]))))

;; I don't know how to test A* yet. Once I figure that out
;; This file will be up to date.