(ns aws-overlord.logic.accounts-test
  (:require [clojure.test :refer :all]
            [aws-overlord.logic.accounts :as accounts]))

(def account
  {:credentials {:key-id "foo"
                 :access-key "bar"}
   :networks [{:region "eu-west-1"
               :cidr-block "10.144.0.0/19"}]})

(deftest test-generate-typed-cidr-blocks
  (is (= (accounts/generate-typed-cidr-blocks "10.144.0.0/19")
         {:public "10.144.0.0/21"
          :shared "10.144.8.0/21"
          :private "10.144.16.0/20"})))

(deftest test-generate-subnets-public
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:public "10.144.0.0/21"])
         [{:type :public
           :availability-zone "eu-west-1a"
           :cidr-block "10.144.0.0/24"}
          {:type :public
           :availability-zone "eu-west-1b"
           :cidr-block "10.144.1.0/24"}
          {:type :public
           :availability-zone "eu-west-1c"
           :cidr-block "10.144.2.0/24"}])))

(deftest test-generate-subnets-shared
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:shared "10.144.8.0/21"])
         [{:type :shared
           :availability-zone "eu-west-1a"
           :cidr-block "10.144.8.0/24"}
          {:type :shared
           :availability-zone "eu-west-1b"
           :cidr-block "10.144.9.0/24"}
          {:type :shared
           :availability-zone "eu-west-1c"
           :cidr-block "10.144.10.0/24"}])))

(deftest test-generate-subnets-private
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:private "10.144.16.0/20"])
         [{:type :private
           :availability-zone "eu-west-1a"
           :cidr-block "10.144.16.0/23"}
          {:type :private
           :availability-zone "eu-west-1b"
           :cidr-block "10.144.18.0/23"}
          {:type :private
           :availability-zone "eu-west-1c"
           :cidr-block "10.144.20.0/23"}])))