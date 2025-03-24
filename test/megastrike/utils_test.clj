(ns megastrike.utils-test
  (:require [megastrike.utils :as sut]
            [clojure.test :as t]))

(t/deftest test-keyword-maker
  (t/testing "One-word keywords"
    (t/is (= (sut/keyword-maker "test") "test"))
    (t/is (= (sut/keyword-maker "Test") "test"))
    (t/is (= (sut/keyword-maker "TEST") "test")))
  (t/testing "Multi-word keywords"
    (t/is (= (sut/keyword-maker "test Keyword") "test-keyword"))
    (t/is (= (sut/keyword-maker "Test keyword") "test-keyword"))
    (t/is (= (sut/keyword-maker "TEST KEYWORD") "test-keyword")))
  (t/testing "MUL Keywords"
    (t/is (= (sut/keyword-maker "s*") "s*"))
    (t/is (= (sut/keyword-maker "MUL ID") "mul-id")))
  (t/testing "Strings beginning and ending with spaces should have the spaces stripped."
    (t/is (= (sut/keyword-maker " strip") "strip"))
    (t/is (= (sut/keyword-maker " strip me") "strip-me"))
    (t/is (= (sut/keyword-maker "strip-me ") "strip-me")))
  (t/testing "Empty strings should return nil"
    (t/is (= (sut/keyword-maker "") nil))))

(t/deftest test-strip-quotes
  (t/testing "Strip quotes"
    (t/is (= (sut/strip-quotes "Test \" String") "Test  String"))))
