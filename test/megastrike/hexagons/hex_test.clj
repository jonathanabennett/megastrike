(ns megastrike.hexagons.hex-test
  (:require
   [clojure.test :as t]
   [megastrike.hexagons.hex :as sut]))


(t/deftest hexagon-equality
  (t/testing "Two hexes should match if they have the same q,r,s address."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) (sut/hexagon 0 0 0)) true)))
  (t/testing "Two hexes should NOT match if they have different q,r,s addresses."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) (sut/hexagon 0 -1 1)) false)))
  (t/testing "s = -q-r in hex creation."
    (t/is (= (sut/same-hex (sut/hexagon 1 0) (sut/hexagon 1 0 -1)) true)))
  (t/testing "Disparate maps should not match."
    (t/is (= (sut/same-hex (sut/hexagon 0 0 0) {:x 0 :y 0}) false))))

(t/deftest hex-addition
  (let [origin-hex (sut/hexagon 0 0 0)
        diff-q-hex (sut/hexagon 1 0 -1)
        diff-r-hex (sut/hexagon 0 1 -1)
        diff-s-hex (sut/hexagon -1 -1 2)]
    (t/testing "Testing hexagon addition."
      (t/is (sut/same-hex (sut/hex-addition origin-hex diff-q-hex)
                         (sut/hexagon 1 0 -1)))
      (t/is (sut/same-hex (sut/hex-addition origin-hex diff-r-hex)
                         (sut/hexagon 0 1 -1)))
      (t/is (sut/same-hex (sut/hex-addition origin-hex diff-s-hex)
                         (sut/hexagon -1 -1 2)))
      (t/is (sut/same-hex (sut/hex-addition diff-q-hex diff-r-hex)
                         (sut/hexagon 1 1 -2))))))

(t/deftest hex-subtraction
  (let [origin-hex (sut/hexagon 0 0 0)
        diff-q-hex (sut/hexagon 1 0 -1)
        diff-r-hex (sut/hexagon 0 1 -1)
        diff-s-hex (sut/hexagon -1 -1 2)]
    (t/testing "Testing hexagon subtraction."
      (t/is (sut/same-hex (sut/hex-subtraction origin-hex diff-q-hex)
                         (sut/hexagon -1 0 1)))
      (t/is (sut/same-hex (sut/hex-subtraction origin-hex diff-r-hex)
                         (sut/hexagon 0 -1 1)))
      (t/is (sut/same-hex (sut/hex-subtraction origin-hex diff-s-hex)
                         (sut/hexagon 1 1 -2)))
      (t/is (sut/same-hex (sut/hex-subtraction diff-q-hex diff-r-hex)
                         (sut/hexagon 1 -1 0))))))

(t/deftest hex-offset-conversion
  (let [test-hex-00 (sut/hexagon 0 0 0)
        test-hex-offset-00 {:x 0 :y 0}
        test-hex-42 (sut/hexagon 4 0 -4)
        test-hex-offset-42 {:x 4 :y 2}]
    (t/testing "Testing from hex to offset."
      (t/is (= test-hex-offset-00 (sut/offset-from-hex test-hex-00)))
      (t/is (not (= test-hex-offset-00 (sut/offset-from-hex test-hex-42))))
      (t/is (= test-hex-offset-42 (sut/offset-from-hex test-hex-42)))
      (t/is (not (= test-hex-offset-42 (sut/offset-from-hex test-hex-00)))))
    (t/testing "Testing hex from offset."
      (t/is (= test-hex-00 (sut/hex-from-offset test-hex-offset-00)))
      (t/is (not (= test-hex-00 (sut/hex-from-offset test-hex-offset-42))))
      (t/is (= test-hex-42 (sut/hex-from-offset test-hex-offset-42)))
      (t/is (not (= test-hex-42 (sut/hex-from-offset test-hex-offset-00)))))))

(t/deftest hex-multiplication
  (let [test-hex (sut/hexagon 4 0 -4)]
    (t/testing "Testing hex multiplication"
      (t/is (= (sut/hex-multiplication test-hex 2) {:q 8 :r 0 :s -8})))))

(t/deftest hex-distance
  (let [test-hex-00 (sut/hexagon 0 0 0)
        test-hex-42 (sut/hexagon 4 0 -4)]
    (t/testing "Measure distance from hex 0,0 to hex 2,4"
      (t/is (= (sut/hex-distance test-hex-00 test-hex-42) 4)))))

(t/deftest hex-neighbor
  (let [test-hex-00 (sut/hexagon 0 0 0)]
    (t/testing "Check each hex neighbor for hex 0,0"
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 0) {:q 1 :r 0 :s -1}))
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 1) {:q 1 :r -1 :s 0}))
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 2) {:q 0 :r -1 :s 1}))
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 3) {:q -1 :r 0 :s 1}))
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 4) {:q -1 :r 0 :s 1}))
      (t/is (sut/same-hex (sut/hex-neighbor test-hex-00 5) {:q 0 :r 1 :s -1})))))

(t/deftest hex-to-pixel
  (let [layout (sut/create-layout)]
    (t/testing "Check the pixel location of hexes."
      (let [test-hex-00 (sut/hexagon 0 0 0)
            test-hex-42 (sut/hexagon 4 0 -4)]
        (t/is (= (sut/hex-to-pixel test-hex-00 layout) {:x 84.0 :y 65.0}))
        (t/is (= (sut/hex-to-pixel test-hex-42 layout) {:x 588.0 :y 314.41531628991834}))))))

(t/deftest pixel-to-hex
  (let [layout (sut/create-layout)]
    (t/testing "Check the hex location of pixels."
      (let [test-hex-00 (sut/hexagon 0 0 0)
            test-hex-42 (sut/hexagon 4 0 -4)]
        (t/is (= (sut/pixel-to-hex {:x 100.0 :y 30.0} layout) (sut/hexagon 0 0 0)))
        (t/is (= (sut/pixel-to-hex {:x 55.0 :y 55.0} layout) (sut/hexagon 0 0 0)))
        (t/is (= (sut/pixel-to-hex {:x 55.0 :y 45.0} layout) (sut/hexagon 0 0 0)))))))

(t/deftest hex-points
  (let [layout (sut/create-layout)
        hex-00-correct (list {:x 168.0, :y 65.0}
                             {:x 125.99999999999997, :y 127.3538290724796}
                             {:x 42.00000000000002, :y 127.3538290724796}
                             {:x 0.0, :y 65.00000000000001}
                             {:x 42.000000000000036, :y 2.6461709275204015}
                             {:x 125.99999999999994, :y 2.6461709275203873})]
    (t/testing "Find all vertices of a hex"
      (let [test-hex-00 (sut/hexagon 0 0 0)]
        (t/is (= (sut/hex-points test-hex-00 layout)
                 hex-00-correct))))))
