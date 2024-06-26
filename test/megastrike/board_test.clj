(ns megastrike.board-test
  (:require [megastrike.board :as sut]
            [clojure.test :as t]))

(t/deftest test-create-tile
  (t/testing "Valid tiles"
    (t/is (= (sut/create-tile 0 0 0 1 "" "Grass")
             {:q 0 :r 0 :s 0 :elevation 1 :terrain "" :palette "Grass"}))
    (t/is (= (sut/create-tile 2 2 1 "" "Grass")
             {:q 2, :r 1, :s -3, :elevation 1, :terrain "", :palette "Grass"}))))

(t/deftest test-parse-hex-line
  (t/testing "Test valid hex lines"
    (t/is (= (sut/parse-hex-line "hex 0101 0 \"ground_fluff:1:2\" \"grass\"") {:q 1, :r 1, :s -2, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0501 0 \"ground_fluff:1:1;water:1\" \"grass\"") {:q 5, :r -1, :s -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0402 0 \"woods:1:20;ground_fluff:3:1;foliage_elev:2\" \"grass\"") {:q 4, :r 0, :s -4, :elevation 0, :terrain "woods:1:20;ground_fluff:3:1;foliage_elev:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1202 3 \"\" \"grass\"") {:q 12, :r -4, :s -8, :elevation 3, :terrain "", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0808 0 \"bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3\" \"grass\"") {:q 8, :r 4, :s -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0312 0 \"road:1:9;ground_fluff:1:1\" \"grass\"") {:q 3, :r 11, :s -14, :elevation 0, :terrain "road:1:9;ground_fluff:1:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1301 -1 \"\" \"fungus\"") {:q 13, :r -5, :s -8, :elevation -1, :terrain "", :palette "fungus"}))))

(t/deftest test-create-board
  (t/testing "Test an empty board."
    (t/is (= (sut/create-board 3 3) [{:q 1, :r 1, :s -2, :elevation 0, :terrain "", :palette "grass"} {:q 1, :r 2, :s -3, :elevation 0, :terrain "", :palette "grass"} {:q 1, :r 3, :s -4, :elevation 0, :terrain "", :palette "grass"} {:q 2, :r 0, :s -2, :elevation 0, :terrain "", :palette "grass"} {:q 2, :r 1, :s -3, :elevation 0, :terrain "", :palette "grass"} {:q 2, :r 2, :s -4, :elevation 0, :terrain "", :palette "grass"} {:q 3, :r 0, :s -3, :elevation 0, :terrain "", :palette "grass"} {:q 3, :r 1, :s -4, :elevation 0, :terrain "", :palette "grass"} {:q 3, :r 2, :s -5, :elevation 0, :terrain "", :palette "grass"}])))
  (t/testing "Test an example board file."
    (t/is (= (sut/create-board "resources/boards/AGoAC Maps/16x17 Grassland 2.board")
             [{:q 1, :r 1, :s -2, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 0, :s -2, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 0, :s -3, :elevation 0, :terrain "", :palette "grass"}
              {:q 4, :r -1, :s -3, :elevation 0, :terrain "", :palette "grass"}
              {:q 5, :r -1, :s -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 6, :r -2, :s -4, :elevation 0, :terrain "", :palette "grass"}
              {:q 7, :r -2, :s -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 8, :r -3, :s -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 9, :r -3, :s -6, :elevation 0, :terrain "", :palette "grass"}
              {:q 10, :r -4, :s -6, :elevation 0, :terrain "", :palette "grass"}
              {:q 11, :r -4, :s -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 12, :r -5, :s -7, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r -5, :s -8, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r -6, :s -8, :elevation 0, :terrain "", :palette "grass"}
              {:q 15, :r -6, :s -9, :elevation 0, :terrain "", :palette "grass"}
              {:q 16, :r -7, :s -9, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 2, :s -3, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 1, :s -3, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 1, :s -4, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 4, :r 0, :s -4, :elevation 0, :terrain "woods:1:20;ground_fluff:3:1;foliage_elev:2", :palette "grass"}
              {:q 5, :r 0, :s -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 6, :r -1, :s -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 7, :r -1, :s -6, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 8, :r -2, :s -6, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
              {:q 9, :r -2, :s -7, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 10, :r -3, :s -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 11, :r -3, :s -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 12, :r -4, :s -8, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r -4, :s -9, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r -5, :s -9, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 15, :r -5, :s -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 16, :r -6, :s -10, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 3, :s -4, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 2, :s -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 2, :s -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 4, :r 1, :s -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 5, :r 1, :s -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 6, :r 0, :s -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 7, :r 0, :s -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
              {:q 8, :r -1, :s -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:q 9, :r -1, :s -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
              {:q 10, :r -2, :s -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 11, :r -2, :s -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 12, :r -3, :s -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:q 13, :r -3, :s -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 14, :r -4, :s -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r -4, :s -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 16, :r -5, :s -11, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 4, :s -5, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 3, :s -5, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 3, :s -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 4, :r 2, :s -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 5, :r 2, :s -7, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 6, :r 1, :s -7, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 7, :r 1, :s -8, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:q 8, :r 0, :s -8, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 9, :r 0, :s -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 10, :r -1, :s -9, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 11, :r -1, :s -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 12, :r -2, :s -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 13, :r -2, :s -11, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:q 14, :r -3, :s -11, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 15, :r -3, :s -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 16, :r -4, :s -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 5, :s -6, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 4, :s -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 4, :s -7, :elevation 0, :terrain "water:1", :palette "grass"}
              {:q 4, :r 3, :s -7, :elevation 2, :terrain "", :palette "grass"}
              {:q 5, :r 3, :s -8, :elevation 2, :terrain "", :palette "grass"}
              {:q 6, :r 2, :s -8, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 7, :r 2, :s -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 8, :r 1, :s -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 9, :r 1, :s -10, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 10, :r 0, :s -10, :elevation 1, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:q 11, :r 0, :s -11, :elevation 2, :terrain "", :palette "grass"}
              {:q 12, :r -1, :s -11, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 13, :r -1, :s -12, :elevation 1, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 14, :r -2, :s -12, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 15, :r -2, :s -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 16, :r -3, :s -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 6, :s -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 2, :r 5, :s -7, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 5, :s -8, :elevation 0, :terrain "water:1", :palette "grass"}
              {:q 4, :r 4, :s -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
              {:q 5, :r 4, :s -9, :elevation 2, :terrain "", :palette "grass"}
              {:q 6, :r 3, :s -9, :elevation 1, :terrain "", :palette "grass"}
              {:q 7, :r 3, :s -10, :elevation 0, :terrain "ground_fluff:1:4;rough:1:20", :palette "grass"}
              {:q 8, :r 2, :s -10, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 9, :r 2, :s -11, :elevation 1, :terrain "", :palette "grass"}
              {:q 10, :r 1, :s -11, :elevation 2, :terrain "", :palette "grass"}
              {:q 11, :r 1, :s -12, :elevation 3, :terrain "", :palette "grass"}
              {:q 12, :r 0, :s -12, :elevation 2, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 13, :r 0, :s -13, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 14, :r -1, :s -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 15, :r -1, :s -14, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 16, :r -2, :s -14, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 7, :s -8, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 2, :r 6, :s -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 3, :r 6, :s -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 4, :r 5, :s -9, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 5, :r 5, :s -10, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:q 6, :r 4, :s -10, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:q 7, :r 4, :s -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 8, :r 3, :s -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 9, :r 3, :s -12, :elevation 1, :terrain "", :palette "grass"}
              {:q 10, :r 2, :s -12, :elevation 0, :terrain "road:1:20;ground_fluff:1:2", :palette "grass"}
              {:q 11, :r 2, :s -13, :elevation 2, :terrain "", :palette "grass"}
              {:q 12, :r 1, :s -13, :elevation 1, :terrain "", :palette "grass"}
              {:q 13, :r 1, :s -14, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 14, :r 0, :s -14, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 0, :s -15, :elevation 0, :terrain "road:1:18;ground_fluff:1:1", :palette "grass"}
              {:q 16, :r -1, :s -15, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 8, :s -9, :elevation 0, :terrain "", :palette "grass"}
              {:q 2, :r 7, :s -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 3, :r 7, :s -10, :elevation 0, :terrain "ground_fluff:1:3;rough:1:20", :palette "grass"}
              {:q 4, :r 6, :s -10, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 5, :r 6, :s -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 6, :r 5, :s -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 7, :r 5, :s -12, :elevation 0, :terrain "ground_fluff:1:3;water:1", :palette "grass"}
              {:q 8, :r 4, :s -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:q 9, :r 4, :s -13, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:q 10, :r 3, :s -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 11, :r 3, :s -14, :elevation 0, :terrain "road:1:36", :palette "grass"}
              {:q 12, :r 2, :s -14, :elevation 0, :terrain "road:1:34", :palette "grass"}
              {:q 13, :r 2, :s -15, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:q 14, :r 1, :s -15, :elevation 1, :terrain "", :palette "grass"}
              {:q 15, :r 1, :s -16, :elevation 0, :terrain "", :palette "grass"}
              {:q 16, :r 0, :s -16, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 9, :s -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 2, :r 8, :s -10, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
              {:q 3, :r 8, :s -11, :elevation 0, :terrain "ground_fluff:1:3", :palette "grass"}
              {:q 4, :r 7, :s -11, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 5, :r 7, :s -12, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
              {:q 6, :r 6, :s -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:2", :palette "grass"}
              {:q 7, :r 6, :s -13, :elevation 0, :terrain "water:1;bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:2", :palette "grass"}
              {:q 8, :r 5, :s -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 9, :r 5, :s -14, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 10, :r 4, :s -14, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 11, :r 4, :s -15, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 3, :s -15, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 3, :s -16, :elevation 2, :terrain "", :palette "grass"}
              {:q 14, :r 2, :s -16, :elevation 1, :terrain "", :palette "grass"}
              {:q 15, :r 2, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 16, :r 1, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 10, :s -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 2, :r 9, :s -11, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 9, :s -12, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 4, :r 8, :s -12, :elevation 0, :terrain "road:1:18;ground_fluff:1:3", :palette "grass"}
              {:q 5, :r 8, :s -13, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}
              {:q 6, :r 7, :s -13, :elevation 2, :terrain "", :palette "grass"}
              {:q 7, :r 7, :s -14, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 8, :r 6, :s -14, :elevation 0, :terrain "woods:1:20;ground_fluff:1:2;foliage_elev:2", :palette "grass"}
              {:q 9, :r 6, :s -15, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 10, :r 5, :s -15, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 11, :r 5, :s -16, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 4, :s -16, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 4, :s -17, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:q 14, :r 3, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 15, :r 3, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 16, :r 2, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 1, :r 11, :s -12, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 10, :s -12, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 10, :s -13, :elevation 0, :terrain "road:1:10;ground_fluff:1:1", :palette "grass"}
              {:q 4, :r 9, :s -13, :elevation 2, :terrain "", :palette "grass"}
              {:q 5, :r 9, :s -14, :elevation 3, :terrain "", :palette "grass"}
              {:q 6, :r 8, :s -14, :elevation 3, :terrain "", :palette "grass"}
              {:q 7, :r 8, :s -15, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 8, :r 7, :s -15, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 9, :r 7, :s -16, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 10, :r 6, :s -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 11, :r 6, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 5, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 5, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r 4, :s -18, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 4, :s -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 16, :r 3, :s -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 12, :s -13, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 2, :r 11, :s -13, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 3, :r 11, :s -14, :elevation 0, :terrain "road:1:9;ground_fluff:1:1", :palette "grass"}
              {:q 4, :r 10, :s -14, :elevation 2, :terrain "", :palette "grass"}
              {:q 5, :r 10, :s -15, :elevation 2, :terrain "", :palette "grass"}
              {:q 6, :r 9, :s -15, :elevation 2, :terrain "", :palette "grass"}
              {:q 7, :r 9, :s -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 8, :r 8, :s -16, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 9, :r 8, :s -17, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 10, :r 7, :s -17, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 11, :r 7, :s -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 12, :r 6, :s -18, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 13, :r 6, :s -19, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r 5, :s -19, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 5, :s -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 16, :r 4, :s -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 13, :s -14, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 2, :r 12, :s -14, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:q 3, :r 12, :s -15, :elevation 0, :terrain "road:1:17", :palette "grass"}
              {:q 4, :r 11, :s -15, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 5, :r 11, :s -16, :elevation 2, :terrain "", :palette "grass"}
              {:q 6, :r 10, :s -16, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 7, :r 10, :s -17, :elevation 1, :terrain "", :palette "grass"}
              {:q 8, :r 9, :s -17, :elevation 1, :terrain "", :palette "grass"}
              {:q 9, :r 9, :s -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 10, :r 8, :s -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 11, :r 8, :s -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 12, :r 7, :s -19, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 7, :s -20, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
              {:q 14, :r 6, :s -20, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 6, :s -21, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 16, :r 5, :s -21, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 14, :s -15, :elevation 0, :terrain "road:1:18", :palette "grass"}
              {:q 2, :r 13, :s -15, :elevation 0, :terrain "", :palette "grass"}
              {:q 3, :r 13, :s -16, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:q 4, :r 12, :s -16, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 5, :r 12, :s -17, :elevation 1, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 6, :r 11, :s -17, :elevation 1, :terrain "", :palette "grass"}
              {:q 7, :r 11, :s -18, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 8, :r 10, :s -18, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 9, :r 10, :s -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 10, :r 9, :s -19, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 11, :r 9, :s -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 12, :r 8, :s -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 13, :r 8, :s -21, :elevation 0, :terrain "woods:1:20;foliage_elev:2", :palette "grass"}
              {:q 14, :r 7, :s -21, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 7, :s -22, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 16, :r 6, :s -22, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 15, :s -16, :elevation 0, :terrain "", :palette "grass"}
              {:q 2, :r 14, :s -16, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 3, :r 14, :s -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 4, :r 13, :s -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 5, :r 13, :s -18, :elevation 0, :terrain "rough:1:20", :palette "grass"}
              {:q 6, :r 12, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 7, :r 12, :s -19, :elevation 0, :terrain "", :palette "grass"}
              {:q 8, :r 11, :s -19, :elevation 0, :terrain "", :palette "grass"}
              {:q 9, :r 11, :s -20, :elevation 0, :terrain "ground_fluff:1:2;water:1", :palette "grass"}
              {:q 10, :r 10, :s -20, :elevation 0, :terrain "", :palette "grass"}
              {:q 11, :r 10, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 9, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 9, :s -22, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r 8, :s -22, :elevation 0, :terrain "ground_fluff:1:1;rough:1:20", :palette "grass"}
              {:q 15, :r 8, :s -23, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 16, :r 7, :s -23, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 16, :s -17, :elevation 0, :terrain "", :palette "grass"}
              {:q 2, :r 15, :s -17, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 3, :r 15, :s -18, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 4, :r 14, :s -18, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 5, :r 14, :s -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 6, :r 13, :s -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 7, :r 13, :s -20, :elevation 0, :terrain "", :palette "grass"}
              {:q 8, :r 12, :s -20, :elevation 0, :terrain "", :palette "grass"}
              {:q 9, :r 12, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 10, :r 11, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 11, :r 11, :s -22, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 10, :s -22, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 10, :s -23, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r 9, :s -23, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 9, :s -24, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
              {:q 16, :r 8, :s -24, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 1, :r 17, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 2, :r 16, :s -18, :elevation 0, :terrain "", :palette "grass"}
              {:q 3, :r 16, :s -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 4, :r 15, :s -19, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 5, :r 15, :s -20, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
              {:q 6, :r 14, :s -20, :elevation 0, :terrain "", :palette "grass"}
              {:q 7, :r 14, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 8, :r 13, :s -21, :elevation 0, :terrain "", :palette "grass"}
              {:q 9, :r 13, :s -22, :elevation 0, :terrain "", :palette "grass"}
              {:q 10, :r 12, :s -22, :elevation 0, :terrain "", :palette "grass"}
              {:q 11, :r 12, :s -23, :elevation 0, :terrain "", :palette "grass"}
              {:q 12, :r 11, :s -23, :elevation 0, :terrain "", :palette "grass"}
              {:q 13, :r 11, :s -24, :elevation 0, :terrain "", :palette "grass"}
              {:q 14, :r 10, :s -24, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 15, :r 10, :s -25, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
              {:q 16, :r 9, :s -25, :elevation 0, :terrain "", :palette "grass"}]))))
