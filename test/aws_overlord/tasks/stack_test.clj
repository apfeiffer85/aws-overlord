(ns aws-overlord.tasks.stack-test
  (:require [clojure.test :refer :all]
            [aws-overlord.tasks.stack :refer :all]
            [clojure.data.json :refer [pprint write-str]]))

(def network
  {:cidr-block "10.144.0.0/19"
   :subnets [{:type :private
              :availability-zone "eu-west-1a"
              :cidr-block "10.144.16.0/23"}
             {:type :private
              :availability-zone "eu-west-1b"
              :cidr-block "10.144.18.0/23"}
             {:type :private
              :availability-zone "eu-west-1c"
              :cidr-block "10.144.20.0/23"}
             {:type :public
              :availability-zone "eu-west-1a"
              :cidr-block "10.144.0.0/24"}
             {:type :public
              :availability-zone "eu-west-1b"
              :cidr-block "10.144.1.0/24"}
             {:type :public
              :availability-zone "eu-west-1c"
              :cidr-block "10.144.2.0/24"}
             {:type :shared
              :availability-zone "eu-west-1a"
              :cidr-block "10.144.8.0/24"}
             {:type :shared
              :availability-zone "eu-west-1b"
              :cidr-block "10.144.9.0/24"}
             {:type :shared
              :availability-zone "eu-west-1c"
              :cidr-block "10.144.10.0/24"}]})

(deftest test-generate-template
  (let [data (generate-template {:name "order"} network)
        json (with-out-str (pprint data :escape-slash false))]
    (spit "resources/generated.json" json)))

