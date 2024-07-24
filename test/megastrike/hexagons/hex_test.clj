(ns megastrike.hexagons.hex-test
  (:require
   [clojure.test :as t]
   [megastrike.hexagons.hex :as sut]))

(t/deftest hexagon-equality
  (t/testing "Two hexes should match if they have the same q,r,s address."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) (sut/hexagon 0 0 0)) true)))
  (t/testing "Two hexes should NOT match if they have different q,r,s addresses."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) (sut/hexagon 0 -1 1)) false)))
  (t/testing "r = -p-q in hex creation." (t/is (= (sut/same-hex (sut/hexagon 1 0) (sut/hexagon 1 0 -1)) true)))
  (t/testing "Disparate maps should not match."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) {:x 0 :y 0}) false))))

(t/deftest addition
  (let [origin-hex (sut/hexagon 0 0 0)
        diff-q-hex (sut/hexagon 1 0 -1)
        diff-r-hex (sut/hexagon 0 1 -1)
        diff-s-hex (sut/hexagon -1 -1 2)]
    (t/testing "Testing hexagon addition."
      (t/is (sut/same-hex (sut/addition origin-hex diff-q-hex)
                         (sut/hexagon 1 0 -1)))
      (t/is (sut/same-hex (sut/addition origin-hex diff-r-hex)
                         (sut/hexagon 0 1 -1)))
      (t/is (sut/same-hex (sut/addition origin-hex diff-s-hex)
                         (sut/hexagon -1 -1 2)))
      (t/is (sut/same-hex (sut/addition diff-q-hex diff-r-hex)
                         (sut/hexagon 1 1 -2))))))

(t/deftest subtraction
  (let [origin-hex (sut/hexagon 0 0 0)
        diff-q-hex (sut/hexagon 1 0 -1)
        diff-r-hex (sut/hexagon 0 1 -1)
        diff-s-hex (sut/hexagon -1 -1 2)]
    (t/testing "Testing hexagon subtraction."
      (t/is (sut/same-hex (sut/subtraction origin-hex diff-q-hex)
                         (sut/hexagon -1 0 1)))
      (t/is (sut/same-hex (sut/subtraction origin-hex diff-r-hex)
                         (sut/hexagon 0 -1 1)))
      (t/is (sut/same-hex (sut/subtraction origin-hex diff-s-hex)
                         (sut/hexagon 1 1 -2)))
      (t/is (sut/same-hex (sut/subtraction diff-q-hex diff-r-hex)
                         (sut/hexagon 1 -1 0))))))

(t/deftest hex-offset-conversion
  (let [test-hex-00 (sut/hexagon 0 0 0)
        test-hex-offset-00 {:x 0 :y 0}
        test-hex-42 (sut/hexagon 4 0 -4)
        test-hex-offset-42 {:x 4 :y 2}]
    (t/testing "Testing from hex to offset."
      (t/is (= test-hex-offset-00 (sut/hex->offset test-hex-00)))
      (t/is (not (= test-hex-offset-00 (sut/hex->offset test-hex-42))))
      (t/is (= test-hex-offset-42 (sut/hex->offset test-hex-42)))
      (t/is (not (= test-hex-offset-42 (sut/hex->offset test-hex-00)))))
    (t/testing "Testing hex from offset."
      (t/is (= test-hex-00 (sut/offset->hex test-hex-offset-00)))
      (t/is (not (= test-hex-00 (sut/offset->hex test-hex-offset-42))))
      (t/is (= test-hex-42 (sut/offset->hex test-hex-offset-42)))
      (t/is (not (= test-hex-42 (sut/offset->hex test-hex-offset-00)))))))

(t/deftest multiplication
  (let [test-hex (sut/hexagon 4 0 -4)]
    (t/testing "Testing hex multiplication"
      (t/is (= (sut/multiplication test-hex 2) {:p 8 :q 0 :r -8})))))

(t/deftest distance
  (let [test-hex-00 (sut/hexagon 0 0 0)
        test-hex-42 (sut/hexagon 4 0 -4)]
    (t/testing "Measure distance from hex 0,0 to hex 2,4"
      (t/is (= (sut/distance test-hex-00 test-hex-42) 4)))))

(t/deftest neighbor
  (let [test-hex-00 (sut/hexagon 0 0 0)
        neighbors (sut/neighbors test-hex-00)]
    (t/testing "Check each hex neighbor for hex 0,0"
      (t/is (sut/same-hex (nth neighbors 0) {:p 1  :q 0  :r -1}))
      (t/is (sut/same-hex (nth neighbors 1) {:p 1  :q -1 :r 0}))
      (t/is (sut/same-hex (nth neighbors 2) {:p 0  :q -1 :r 1}))
      (t/is (sut/same-hex (nth neighbors 3) {:p -1 :q 0  :r 1}))
      (t/is (sut/same-hex (nth neighbors 4) {:p -1 :q 1  :r 0}))
      (t/is (sut/same-hex (nth neighbors 5) {:p 0  :q 1  :r -1})))))

(t/deftest to-pixel
  (let [layout (sut/create-layout)]
    (t/testing "Check the pixel location of hexes."
      (let [test-00 (sut/hexagon 0 0 0) 
            test-pixels-00 (sut/hex->pixel test-00 layout)
            test-42 (sut/hexagon 4 0 -4)
            test-pixels-42 (sut/hex->pixel test-42 layout)] 
        (t/is (< (abs (- (:x test-pixels-00) 84)) 1))
        (t/is (< (abs (- (:y test-pixels-00) 65)) 1))
        (t/is (< (abs (- (:x test-pixels-42) 588)) 1))
        (t/is (< (abs (- (:y test-pixels-42) 314.4)) 1))))))

(t/deftest points
  (let [layout (sut/create-layout)
        hex-00-correct [168.0 65.0 
                        125.99 127.35 
                        42.00 127.35 
                        0.0 65.00 
                        42.00 2.64 
                        125.99 2.64]]
    (t/testing "Find all vertices of a hex"
      (let [test-00 (sut/points (sut/hexagon 0 0 0) layout)] 
        (t/are [c-point t-point] (< (abs (- c-point t-point)) 1) 
          (nth hex-00-correct 0)  (nth test-00 0)
          (nth hex-00-correct 1)  (nth test-00 1)
          (nth hex-00-correct 2)  (nth test-00 2)
          (nth hex-00-correct 3)  (nth test-00 3)
          (nth hex-00-correct 4)  (nth test-00 4)
          (nth hex-00-correct 5)  (nth test-00 5)
          (nth hex-00-correct 6)  (nth test-00 6)
          (nth hex-00-correct 7)  (nth test-00 7)
          (nth hex-00-correct 8)  (nth test-00 8)
          (nth hex-00-correct 9)  (nth test-00 9)
          (nth hex-00-correct 10) (nth test-00 10)
          (nth hex-00-correct 11) (nth test-00 11)
          )))))

(t/deftest round
  (let [hex-00 (sut/hexagon 0 0 0)
        ppoint {:p 0.2 :q -0.3 :r 0.1}
        qpoint {:p -0.3 :q 0.2 :r 0.1}
        rpoint {:p 0.1 :q -0.3 :r 0.2}
        rqpoint {:p -0.3 :q 0.1 :r 0.2}
        elsepoint {:p 0.2 :q -0.2 :r 0.0}]
    (t/is (sut/same-hex hex-00 (sut/round ppoint)))
    (t/is (sut/same-hex hex-00 (sut/round qpoint)))
    (t/is (sut/same-hex hex-00 (sut/round rpoint)))
    (t/is (sut/same-hex hex-00 (sut/round rqpoint)))
    (t/is (sut/same-hex hex-00 (sut/round elsepoint)))))

(t/deftest facing
  (let [hex-42 (sut/hexagon 4 0 -4)
        layout (sut/create-layout)
        n-dest {:x 588 :y 200}
        ne-dest {:x 688 :y 250}
        se-dest {:x 688 :y 350}
        s-dest {:x 588 :y 400}
        sw-dest {:x 488 :y 350}
        nw-dest {:x 488 :y 250}]
    (t/is (= (sut/facing hex-42 n-dest layout) :n))
    (t/is (= (sut/facing hex-42 ne-dest layout) :ne))
    (t/is (= (sut/facing hex-42 se-dest layout) :se))
    (t/is (= (sut/facing hex-42 s-dest layout) :s))
    (t/is (= (sut/facing hex-42 sw-dest layout) :sw))
    (t/is (= (sut/facing hex-42 nw-dest layout) :nw))))
