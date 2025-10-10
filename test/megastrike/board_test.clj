(ns megastrike.board-test
  (:require [megastrike.board :as sut]
            [clojure.test :as t]))

(t/deftest test-create-tile
  (t/testing "Valid tiles"
    (t/is (= (sut/create-tile 0 0 0 1 "" "Grass")
             {:hex/p 0 :hex/q 0 :hex/r 0 :elevation 1 :terrain "" :palette "Grass"}))
    (t/is (= (sut/create-tile 2 2 1 "" "Grass")
             {:hex/p 2, :hex/q 1, :hex/r -3, :elevation 1, :terrain "", :palette "Grass"}))))

(t/deftest test-parse-hex-line
  (t/testing "Test valid hex lines"
    (t/is (= (sut/parse-hex-line "hex 0101 0 \"ground_fluff:1:2\" \"grass\"")
             {:hex/p 1, :hex/q 1, :hex/r -2, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0501 0 \"ground_fluff:1:1;water:1\" \"grass\"")
             {:hex/p 5, :hex/q -1, :hex/r -4, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0402 0 \"woods:1:20;ground_fluff:3:1;foliage_elev:2\" \"grass\"")
             {:hex/p 4, :hex/q 0, :hex/r -4, :elevation 0, :terrain "woods:1:20;ground_fluff:3:1;foliage_elev:2", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1202 3 \"\" \"grass\"")
             {:hex/p 12, :hex/q -4, :hex/r -8, :elevation 3, :terrain "", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0808 0 \"bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3\" \"grass\"")
             {:hex/p 8, :hex/q 4, :hex/r -12, :elevation 0, :terrain "bridge:3:18;bridge_cf:90;bridge_elev:1;ground_fluff:1:3", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 0312 0 \"road:1:9;ground_fluff:1:1\" \"grass\"")
             {:hex/p 3, :hex/q 11, :hex/r -14, :elevation 0, :terrain "road:1:9;ground_fluff:1:1", :palette "grass"}))
    (t/is (= (sut/parse-hex-line "hex 1301 -1 \"\" \"fungus\"")
             {:hex/p 13, :hex/q -5, :hex/r -8, :elevation -1, :terrain "", :palette "fungus"}))))

(t/deftest test-hex-line
  (t/testing "Test drawing a straight line for LOS purposes"
    (let [board (sut/create-board "data/boards/AGoAC Maps/16x17 Grassland 2.board")
          start1 {:hex/p 4 :hex/q 4 :hex/r -8}
          end1 {:hex/p 6 :hex/q 0 :hex/r -6}
          start2 {:hex/p 2 :hex/q 2 :hex/r -4}
          end2 {:hex/p 15 :hex/q -4 :hex/r -11}]
      (t/is (= (sut/line start1 end1 board)
               [{:hex/p 4, :hex/q 4, :hex/r -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 5, :hex/q 3, :hex/r -8, :elevation 2, :terrain "", :palette "grass"}
                {:hex/p 5, :hex/q 2, :hex/r -7, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 6, :hex/q 1, :hex/r -7, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 6, :hex/q 0, :hex/r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}]))
      (t/is (= (sut/line start2 end2 board)
               [{:hex/p 2, :hex/q 2, :hex/r -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 3, :hex/q 2, :hex/r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:hex/p 4, :hex/q 1, :hex/r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 5, :hex/q 1, :hex/r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 6, :hex/q 0, :hex/r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 7, :hex/q 0, :hex/r -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
                {:hex/p 8, :hex/q -1, :hex/r -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:hex/p 9, :hex/q -1, :hex/r -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
                {:hex/p 10, :hex/q -2, :hex/r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 11, :hex/q -2, :hex/r -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 12, :hex/q -3, :hex/r -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
                {:hex/p 13, :hex/q -3, :hex/r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:hex/p 14, :hex/q -4, :hex/r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 15, :hex/q -4, :hex/r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}]))
      (t/is (= (sut/line start1 end2 board)
               [{:hex/p 4, :hex/q 4, :hex/r -8, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 5, :hex/q 3, :hex/r -8, :elevation 2, :terrain "", :palette "grass"}
                {:hex/p 6, :hex/q 3, :hex/r -9, :elevation 1, :terrain "", :palette "grass"}
                {:hex/p 7, :hex/q 2, :hex/r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
                {:hex/p 8, :hex/q 1, :hex/r -9, :elevation 0, :terrain "ground_fluff:1:4", :palette "grass"}
                {:hex/p 9, :hex/q 0, :hex/r -9, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 10, :hex/q 0, :hex/r -10, :elevation 1, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:hex/p 11, :hex/q -1, :hex/r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 12, :hex/q -2, :hex/r -10, :elevation 1, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 13, :hex/q -3, :hex/r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:hex/p 14, :hex/q -3, :hex/r -11, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 15, :hex/q -4, :hex/r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}]))
      (t/is (= (sut/line start2 end2 board)
               [{:hex/p 2, :hex/q 2, :hex/r -4, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 3, :hex/q 2, :hex/r -5, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:hex/p 4, :hex/q 1, :hex/r -5, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 5, :hex/q 1, :hex/r -6, :elevation 0, :terrain "ground_fluff:1:1;water:1", :palette "grass"}
                {:hex/p 6, :hex/q 0, :hex/r -6, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 7, :hex/q 0, :hex/r -7, :elevation 0, :terrain "ground_fluff:3:2", :palette "grass"}
                {:hex/p 8, :hex/q -1, :hex/r -7, :elevation 0, :terrain "woods:1:20;ground_fluff:1:1;foliage_elev:2", :palette "grass"}
                {:hex/p 9, :hex/q -1, :hex/r -8, :elevation 0, :terrain "woods:2:20;foliage_elev:2", :palette "grass"}
                {:hex/p 10, :hex/q -2, :hex/r -8, :elevation 0, :terrain "ground_fluff:1:2", :palette "grass"}
                {:hex/p 11, :hex/q -2, :hex/r -9, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 12, :hex/q -3, :hex/r -9, :elevation 0, :terrain "rough:1:20", :palette "grass"}
                {:hex/p 13, :hex/q -3, :hex/r -10, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}
                {:hex/p 14, :hex/q -4, :hex/r -10, :elevation 0, :terrain "ground_fluff:1:1", :palette "grass"}
                {:hex/p 15, :hex/q -4, :hex/r -11, :elevation 0, :terrain "ground_fluff:3:1", :palette "grass"}])))))

(t/deftest test-distance-from-edge
  (t/testing "Test an empty board."
    (t/is (= (:tiles (sut/create-board 3 3))
             [{:elevation 0, :palette "grass", :terrain "", :hex/p 1, :hex/q 1, :hex/r -2}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 1, :hex/q 2, :hex/r -3}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 1, :hex/q 3, :hex/r -4}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 2, :hex/q 0, :hex/r -2}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 2, :hex/q 1, :hex/r -3}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 2, :hex/q 2, :hex/r -4}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 3, :hex/q 0, :hex/r -3}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 3, :hex/q 1, :hex/r -4}
              {:elevation 0, :palette "grass", :terrain "", :hex/p 3, :hex/q 2, :hex/r -5}])))
  (t/testing "Test coordinate consistency for generated board."
    (let [board (sut/create-board 16 17)
          tiles (:tiles board)]
      ;; Check that all tiles have valid cubic coordinates (p + q + r = 0)
      (t/is (every? #(= (+ (:hex/p %) (:hex/q %) (:hex/r %)) 0) tiles))
      ;; Check that p coordinates are within bounds (1 to width)
      (t/is (every? #(<= 1 (:hex/p %) 16) tiles))
      ;; Check that q coordinates are within reasonable bounds for a 17-height board
      ;; (q ranges from -7 to 17 based on the example data)
      (t/is (every? #(<= -7 (:hex/q %) 17) tiles))))
  (t/testing "Test tile uniqueness for generated board."
    (let [board (sut/create-board 16 17)
          tiles (:tiles board)
          coordinates (map #(select-keys % [:hex/p :hex/q :hex/r]) tiles)]
      ;; Check that all tiles have unique coordinates
      (t/is (= (count coordinates) (count (distinct coordinates))))
      ;; Check that the number of tiles matches expected (width * height)
      (t/is (= (* 16 17) (count tiles)))))
  (t/testing "Test tile distance measurement"
    (let [board (sut/create-board 16 17)]
      (t/is (= (sut/hex-distance-to-edge {:hex/p 2 :hex/q 0 :hex/r -2} board :north) 0))
      (t/is (= (sut/hex-distance-to-edge {:hex/p 2 :hex/q 0 :hex/r -2} board :east) 14))
      (t/is (= (sut/hex-distance-to-edge {:hex/p 2 :hex/q 0 :hex/r -2} board :south) 16))
      (t/is (= (sut/hex-distance-to-edge {:hex/p 2 :hex/q 0 :hex/r -2} board :west) 1)))))
