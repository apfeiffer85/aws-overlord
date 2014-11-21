(ns aws-overlord.tasks.vpc
  (:require [amazonica.aws.ec2 :as ec2]
            [aws-overlord.aws :refer [aws]]
            [clojure.tools.logging :as log]))

(defn- get-vpc-id [credentials cidr-block]
  (let [vpcs (aws ec2/describe-vpcs credentials :filters [{:cidr cidr-block}])]
    (log/info "Found [" (count vpcs) "] VPCs for CIDR block" cidr-block)
    (-> vpcs first :vpc-id)))

(defn- create-vpc-if-not-exists [credentials cidr-block]
  (or (get-vpc-id credentials cidr-block)
      ; TODO wait for available/pending?
      (let [vpc (aws ec2/create-vpc credentials :cidr-block cidr-block)
            vpc-id (:vpc-id vpc)]
        (log/info "Created VPC [" vpc-id "] for CIDR block" cidr-block)
        vpc-id)))

(defn ^:region-dependent create-vpcs [{:keys [credentials network] :as context}]
  ; TODO logging binding to always include account?
  (log/info "Enforcing VPC in [" (:network/region network) "] on account" (get-in context [:account :account/name]))
  (let [{:keys [network/cidr-block]} network
        vpc-id (create-vpc-if-not-exists credentials cidr-block)]
    (assoc-in context [:network :vpc-id] vpc-id)))
