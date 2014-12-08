(ns aws-overlord.tasks.dns-test
  (:require [clojure.test :refer :all]
            [conjure.core :refer :all]
            [aws-overlord.tasks.dns :as dns]
            [aws-overlord.verify :refer :all]
            [amazonica.aws.route53 :as route53]))

(deftest test-dns
  (mocking
    [route53/create-hosted-zone]
    (stubbing
      [route53/list-hosted-zones [{:name "bar.aws.zalando."}
                                  {:name "bar.aws.zalando.net."}]]
      (dns/run {:name "foo"})
      (verify-nth-call-args-for-indices 0 route53/create-hosted-zone [0 1] :name "foo.aws.zalando.")
      (verify-nth-call-args-for-indices 1 route53/create-hosted-zone [0 1] :name "foo.aws.zalando.net.")
      (verify-call-times-for route53/create-hosted-zone 2))))

(deftest test-dns-already-exists
  (mocking
    [route53/create-hosted-zone]
    (stubbing
      [route53/list-hosted-zones {:hosted-zones [{:name "foo.aws.zalando."}
                                                 {:name "foo.aws.zalando.net."}]}]
      (dns/run {:name "foo"})
      (verify-call-times-for route53/create-hosted-zone 0))))
