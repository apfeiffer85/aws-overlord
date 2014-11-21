(ns aws-overlord.enforcer-test
  (:require [clojure.test :refer :all]
            [aws-overlord.aws :refer [with-interceptors]]
            [aws-overlord.enforcer :refer :all]
            [amazonica.aws.ec2 :as ec2]
            [clojure.tools.logging :as log]))

(def ^:private accounts
  [{:account/name "foo"
    :account/key-id "key-id"
    :account/access-key "access-key"
    :account/networks [{:network/region "eu-west-1"
                        :network/cidr-block "10.17.0.0/19"
                        :network/subnets [{:subnet/availability-zone "eu-west-1a"
                                           :subnet/cidr-block "10.17.1.0/17"}
                                          {:subnet/availability-zone "eu-west-1b"
                                           :subnet/cidr-block "10.17.2.0/17"}
                                          {:subnet/availability-zone "eu-west-1c"
                                           :subnet/cidr-block "10.17.3.0/17"}]}]
    :account/owner-email "d.fault@example.com"}
   {:account/name "bar"
    :account/key-id "key-id"
    :account/access-key "access-key"
    :account/networks [{:network/region "eu-west-1"
                        :network/cidr-block "10.18.0.0/19"
                        :network/subnets [{:subnet/availability-zone "eu-west-1a"
                                           :subnet/cidr-block "10.18.1.0/17"}
                                          {:subnet/availability-zone "eu-west-1b"
                                           :subnet/cidr-block "10.18.2.0/17"}
                                          {:subnet/availability-zone "eu-west-1c"
                                           :subnet/cidr-block "10.18.3.0/17"}]}]
    :account/owner-email "d.fault@example.com"}])

(def ^:private default-interceptors
  {ec2/describe-key-pairs (constantly [])
   ec2/create-key-pair (constantly {:key-name "key"})
   ec2/describe-vpcs (constantly [])
   ec2/create-vpc (constantly {:vpc-id "vpc"})
   ec2/describe-subnets (constantly [])
   ec2/create-subnet (constantly {:subnet-id "subnet"})})

(deftest test-enforce
  (with-interceptors
    default-interceptors
    (doseq [account accounts]
      (let [context (enforce-account account)]
        (is (= "vpc" (get-in context [:account :account/networks 0 :vpc-id])))))))
