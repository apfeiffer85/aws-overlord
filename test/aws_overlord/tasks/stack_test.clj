(ns aws-overlord.tasks.stack-test
  (:require [clojure.test :refer :all]
            [aws-overlord.tasks.stack :refer :all]
            [clojure.data.json :refer [pprint write-str]]))

(def network
  {:network/cidr-block "10.144.0.0/19"
   :network/subnets [{:subnet/type :private
                      :subnet/availability-zone "eu-west-1a"
                      :subnet/cidr-block "10.144.16.0/23"}
                     {:subnet/type :private
                      :subnet/availability-zone "eu-west-1b"
                      :subnet/cidr-block "10.144.18.0/23"}
                     {:subnet/type :private
                      :subnet/availability-zone "eu-west-1c"
                      :subnet/cidr-block "10.144.20.0/23"}
                     {:subnet/type :public
                      :subnet/availability-zone "eu-west-1a"
                      :subnet/cidr-block "10.144.0.0/24"}
                     {:subnet/type :public
                      :subnet/availability-zone "eu-west-1b"
                      :subnet/cidr-block "10.144.1.0/24"}
                     {:subnet/type :public
                      :subnet/availability-zone "eu-west-1c"
                      :subnet/cidr-block "10.144.2.0/24"}
                     {:subnet/type :shared
                      :subnet/availability-zone "eu-west-1a"
                      :subnet/cidr-block "10.144.8.0/24"}
                     {:subnet/type :shared
                      :subnet/availability-zone "eu-west-1b"
                      :subnet/cidr-block "10.144.9.0/24"}
                     {:subnet/type :shared
                      :subnet/availability-zone "eu-west-1c"
                      :subnet/cidr-block "10.144.10.0/24"}]})

(deftest test-generate-template
  (let [data (generate-template {:account/name "order"} network)
        json (with-out-str (pprint data :escape-slash false))]
    (println json)
    (println)
    (doseq [resource (sort (keys (get-in data ["Resources"])))]
      (println resource))
    (spit "resources/generated.json" json)))

