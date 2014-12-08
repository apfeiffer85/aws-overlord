(ns aws-overlord.tasks.networking-test
  (:require [clojure.test :refer :all]
            [aws-overlord.tasks.networking :refer :all]
            [clojure.data.json :refer [pprint write-str]]))

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
  (let [data (generate-template {:name "asa"} network)]
    (spit "resources/generated.json" (with-out-str (pprint data :escape-slash false)))))

