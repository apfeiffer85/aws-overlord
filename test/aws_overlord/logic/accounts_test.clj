(ns aws-overlord.logic.accounts-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [amazonica.aws.ec2 :as ec2]
            [amazonica.aws.identitymanagement :as iam]
            [aws-overlord.tasks.security :as security]
            [aws-overlord.tasks.dns :as dns]
            [aws-overlord.tasks.networking :as networking]
            [aws-overlord.tasks.peering :as peering]
            [aws-overlord.logic.accounts :as accounts]))

(deftest test-generate-typed-cidr-blocks
  (is (= (accounts/generate-typed-cidr-blocks "10.2.0.0/19")
         {:public "10.2.0.0/21"
          :shared "10.2.8.0/21"
          :private "10.2.16.0/20"})))

(deftest test-generate-subnets-public
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:public "10.2.0.0/21"])
         [{:type :public
           :availability-zone "eu-west-1a"
           :cidr-block "10.2.0.0/24"}
          {:type :public
           :availability-zone "eu-west-1b"
           :cidr-block "10.2.1.0/24"}
          {:type :public
           :availability-zone "eu-west-1c"
           :cidr-block "10.2.2.0/24"}])))

(deftest test-generate-subnets-shared
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:shared "10.2.8.0/21"])
         [{:type :shared
           :availability-zone "eu-west-1a"
           :cidr-block "10.2.8.0/24"}
          {:type :shared
           :availability-zone "eu-west-1b"
           :cidr-block "10.2.9.0/24"}
          {:type :shared
           :availability-zone "eu-west-1c"
           :cidr-block "10.2.10.0/24"}])))

(deftest test-generate-subnets-private
  (is (= (accounts/generate-subnets ["eu-west-1a" "eu-west-1b" "eu-west-1c"] [:private "10.2.16.0/20"])
         [{:type :private
           :availability-zone "eu-west-1a"
           :cidr-block "10.2.16.0/23"}
          {:type :private
           :availability-zone "eu-west-1b"
           :cidr-block "10.2.18.0/23"}
          {:type :private
           :availability-zone "eu-west-1c"
           :cidr-block "10.2.20.0/23"}])))


(def account
  {:key-id "foo"
   :access-key "bar"
   :networks [{:region "eu-west-1"
               :cidr-block "10.2.0.0/19"}
              {:region "eu-central-1"
               :cidr-block "10.2.32.0/19"}]})

(def an-account
  {:key-id "foo"
   :access-key "bar"
   :name "team"
   :aws-id "123456789000"
   :networks [{:region "eu-west-1"
               :cidr-block "10.2.0.0/19"
               :private-key "private key"
               :subnets #{{:type :public, :availability-zone "eu-west-1a", :cidr-block "10.2.0.0/24"}
                          {:type :public, :availability-zone "eu-west-1b", :cidr-block "10.2.1.0/24"}
                          {:type :public, :availability-zone "eu-west-1c" :cidr-block "10.2.2.0/24"}
                          {:type :shared, :availability-zone "eu-west-1a" :cidr-block "10.2.8.0/24"}
                          {:type :shared, :availability-zone "eu-west-1b", :cidr-block "10.2.9.0/24"}
                          {:type :shared, :availability-zone "eu-west-1c", :cidr-block "10.2.10.0/24"}
                          {:type :private, :availability-zone "eu-west-1a", :cidr-block "10.2.16.0/23"}
                          {:type :private, :availability-zone "eu-west-1b", :cidr-block "10.2.18.0/23"}
                          {:type :private, :availability-zone "eu-west-1c", :cidr-block "10.2.20.0/23"}}}
              {:region "eu-central-1"
               :cidr-block "10.2.32.0/19"
               :private-key "private key"
               :subnets #{{:type :public, :availability-zone "eu-central-1a", :cidr-block "10.2.32.0/24"}
                          {:type :public, :availability-zone "eu-central-1b", :cidr-block "10.2.33.0/24"}
                          {:type :public, :availability-zone "eu-central-1c" :cidr-block "10.2.34.0/24"}
                          {:type :shared, :availability-zone "eu-central-1a" :cidr-block "10.2.40.0/24"}
                          {:type :shared, :availability-zone "eu-central-1b", :cidr-block "10.2.41.0/24"}
                          {:type :shared, :availability-zone "eu-central-1c", :cidr-block "10.2.42.0/24"}
                          {:type :private, :availability-zone "eu-central-1a", :cidr-block "10.2.48.0/23"}
                          {:type :private, :availability-zone "eu-central-1b", :cidr-block "10.2.50.0/23"}
                          {:type :private, :availability-zone "eu-central-1c", :cidr-block "10.2.52.0/23"}}}]
   })

(defn- availability-zones [region]
  (mapv #(hash-map :zone-name (str region %)) [\a \b \c]))

(deftest test-accounts-prepare
  (fact "Should create key pair and generate subnets"
        (accounts/prepare "team" account) => an-account
        (provided
          (ec2/describe-availability-zones) =streams=> [(availability-zones "eu-west-1")
                                                        (availability-zones "eu-central-1")]
          (ec2/describe-key-pairs :key-names anything) => []
          (ec2/create-key-pair :key-name anything) => {:key-material "private key"}
          (iam/get-user) => {:arn "arn:aws:iam::123456789000:user/overlord"}))
  (fact "An existing key pair should raise an error"
        (accounts/prepare "team" account) => (throws IllegalStateException)
        (provided
          (ec2/describe-availability-zones) => (availability-zones "eu-west-1")
          (ec2/describe-key-pairs :key-names anything) => [{:key-name "overlord" :key-fingerprint "foo"}])))

(deftest test-accounts-configure
  (let [account (assoc account :name "team")]
    (fact "Configure should execute tasks in background"
          (deref (accounts/configure account [account])) => nil
          (provided
            (security/run account) => nil
            (dns/run account) => nil
            (networking/run account anything) => nil :times 2
            (peering/run account anything [account]) => nil :times 2))
    (let [account (update-in account [:networks] empty)]
      (fact "Accounts without networks should skip region wide configurations"
            (deref (accounts/configure account [])) => nil
            (provided
              (security/run account) => nil
              (dns/run account) => nil
              (networking/run account anything) => nil :times 0
              (peering/run account anything []) => nil :times 0)))))