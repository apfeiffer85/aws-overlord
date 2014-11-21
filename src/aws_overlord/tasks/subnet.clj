(ns aws-overlord.tasks.subnet
  (:require [amazonica.aws.ec2 :as ec2]
            [aws-overlord.aws :refer [aws]]
            [aws-overlord.tasks.vpc :refer [create-vpcs]]
            [clojure.tools.logging :as log]))

(defn- get-subnet-id [credentials vpc-id {:keys [subnet/availability-zone subnet/cidr-block]}]
  (-> (aws ec2/describe-subnets credentials :filters [{:vpc-id vpc-id
                                                       :availability-zone availability-zone
                                                       :cidr-block cidr-block}])
      first :subnet-id))

(defn- create-subnet-if-not-exists [credentials vpc-id {:keys [subnet/availability-zone subnet/cidr-block] :as subnet}]
  (or (get-subnet-id credentials vpc-id subnet)
      (-> (aws ec2/create-subnet
               :vpc-id vpc-id
               :availability-zone availability-zone
               :cidr-block cidr-block)
          :subnet-id)))

(defn- update-subnet [credentials vpc-id subnet]
  (assoc subnet :subnet-id (create-subnet-if-not-exists credentials vpc-id subnet)))

(defn ^:region-dependent ^{:requires [create-vpcs]} create-subnets
  [{:keys [credentials network] :as context}]
  (log/info "Enforcing subnets in" (:network/region network) "on account" (get-in context [:account :account/name]))
  (let [{:keys [network/vpc-id network/subnets]} network]
    (assoc-in context
              [:network :network/subnets]
              (map (partial update-subnet credentials vpc-id) subnets))))

