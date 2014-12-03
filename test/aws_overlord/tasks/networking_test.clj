(ns aws-overlord.tasks.networking-test
  (:require [clojure.test :refer :all]
            [aws-overlord.tasks.networking :refer :all]
            [clojure.data.json :refer [pprint write-str]]))

(def network
  {:cidr-block "10.144.0.0/19"
   :vpn-gateway-ip "62.138.85.241"
   :vpn-routes ["10.10.0.0/16"
                "10.64.0.0/16"
                "10.160.0.0/11"
                "10.228.0.0/15"]
   :name-servers ["10.144.137.5",
                  "10.144.138.5",
                  "10.144.136.5"]
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
  (let [data (generate-template {:name "asa"} network)]
    (spit "resources/generated.json" (with-out-str (pprint data :escape-slash false)))))

