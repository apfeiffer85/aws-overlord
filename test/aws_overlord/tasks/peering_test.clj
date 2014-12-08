(ns aws-overlord.tasks.peering-test
  (:require [clojure.test :refer :all]
            [conjure.core :refer :all]
            [aws-overlord.tasks.peering :as peering]
            [aws-overlord.verify :refer :all]
            [amazonica.aws.ec2 :as ec2]))

(deftest test-run-first-account
  (mocking
    [ec2/create-vpc-peering-connection
     ec2/accept-vpc-peering-connection
     ec2/create-route-table]
    (peering/run {} {} [])
    (verify-call-times-for ec2/create-vpc-peering-connection 0)
    (verify-call-times-for ec2/accept-vpc-peering-connection 0)
    (verify-call-times-for ec2/create-route-table 0)))

(deftest test-run-one-pair
  (mocking
    [ec2/accept-vpc-peering-connection
     ec2/create-route]
    (stubbing
      [ec2/describe-vpcs [{:vpc-id "vpc"}]
       ec2/describe-vpc-peering-connections []
       ec2/describe-route-tables (mapv #(hash-map :route-table-id (str "rtb-" %)) (range 1 7))
       ec2/create-vpc-peering-connection {:vpc-peering-connection-id "pcx-123"}]
      (peering/run {:aws-id "foo"
                    :name "foo"}
                   {:region "eu-west-1"
                    :cidr-block "10.2.32.0/19"}
                   [{:aws-id "bar"
                     :name "bar"
                     :networks [{:region "eu-west-1"
                                 :cidr-block "10.2.0.0/19"}]}])
      (verify-call-times-for ec2/create-vpc-peering-connection 1)
      (verify-call-times-for ec2/accept-vpc-peering-connection 1)
      (verify-call-times-for ec2/create-route 12))))
