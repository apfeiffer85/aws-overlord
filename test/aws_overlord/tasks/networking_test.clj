(ns aws-overlord.tasks.networking-test
  (:import (com.amazonaws AmazonServiceException))
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [amazonica.aws.cloudformation :as cf]
            [aws-overlord.tasks.networking :refer :all]))

(def network
  {:cidr-block "10.2.0.0/19"
   :vpn-gateway-ip "93.184.216.119"
   :vpn-routes ["10.11.0.0/16"
                "10.47.0.0/16"
                "10.170.0.0/11"
                "10.234.0.0/15"]
   :name-servers ["10.2.131.5",
                  "10.2.132.5",
                  "10.2.133.5"]
   :subnets [{:type :private
              :availability-zone "eu-west-1a"
              :cidr-block "10.2.16.0/23"}
             {:type :private
              :availability-zone "eu-west-1b"
              :cidr-block "10.2.18.0/23"}
             {:type :private
              :availability-zone "eu-west-1c"
              :cidr-block "10.2.20.0/23"}
             {:type :public
              :availability-zone "eu-west-1a"
              :cidr-block "10.2.0.0/24"}
             {:type :public
              :availability-zone "eu-west-1b"
              :cidr-block "10.2.1.0/24"}
             {:type :public
              :availability-zone "eu-west-1c"
              :cidr-block "10.2.2.0/24"}
             {:type :shared
              :availability-zone "eu-west-1a"
              :cidr-block "10.2.8.0/24"}
             {:type :shared
              :availability-zone "eu-west-1b"
              :cidr-block "10.2.9.0/24"}
             {:type :shared
              :availability-zone "eu-west-1c"
              :cidr-block "10.2.10.0/24"}]})

(deftest test-generate-template
  (let [template (generate-template {:name "team"} network)]
    ; TODO better assertions
    (is (not (empty? template)))))

(deftest test-generate-template-no-name-servers
  (let [template (generate-template {:name "team"} (update-in network [:name-servers] empty))]
    ; TODO better assertions
    (is (not (empty? template)))))

(deftest test-networking
  (fact "Missing stack should be created"
        (run {:name "team"} network :sleep-timeout 100) => nil
        (provided
          (cf/describe-stacks :stack-name "overlord") =streams=> [[]
                                                                  [{:stack-name "overlord"
                                                                    :stack-status "CREATE_IN_PROGRESS"}]
                                                                  [{:stack-name "overlord"
                                                                    :stack-status "CREATE_COMPLETE"}]]
          (cf/create-stack :stack-name anything :template-body anything) => {})))

(deftest test-networking-stack-exists
  (fact "Existing stack shouldn't be touched"
        (run {:name "team"} network) => nil
        (provided
          (cf/describe-stacks :stack-name "overlord") => [{:stack-name "overlord"}])))

(deftest test-networking-failed
  (fact "A failed stack creation should be reported"
        (run {:name "team"} network) => (throws IllegalStateException)
        (provided
          (cf/describe-stacks :stack-name "overlord") =streams=> [[]
                                                                  [{:stack-name "overlord"
                                                                    :stack-status "CREATE_FAILED"}]]
          (cf/create-stack :stack-name anything :template-body anything) => {})))