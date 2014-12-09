(ns aws-overlord.aws-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [aws-overlord.aws :refer :all]))

(deftest test-aws
  (is (= [nil nil] (credentials)))
  (switch-to
    "eu-west-1"
    (is (= [nil nil "eu-west-1"] (credentials)))
    (login-to
      {:key-id "foo" :access-key "bar"}
      (is (= ["foo" "bar" "eu-west-1"] (credentials)))
      (switch-to
        "eu-central-1"
        (is (= ["foo" "bar" "eu-central-1"] (credentials))))
      (is (= ["foo" "bar" "eu-west-1"] (credentials))))
    (is (= [nil nil "eu-west-1"] (credentials))))
  (is (= [nil nil] (credentials))))
