(ns aws-overlord.logic.accounts
  (:import (com.amazonaws AmazonServiceException))
  (:require [aws-overlord.net :refer :all]
            [clojure.string :refer [split]]
            [amazonica.core :refer [with-credential]]
            [amazonica.aws.ec2 :as ec2]
            [amazonica.aws.identitymanagement :as iam]
            [clojure.tools.logging :as log]))

(defn- list-availability-zones []
  (mapv :zone-name (ec2/describe-availability-zones)))

(defn- get-account-id []
  (let [user (iam/get-user)]
    (log/info "Fetching account-id from" user)
    (nth (split (:arn user) #":") 4)))

(defn- key-pair-exists? [name]
  (try
    (not (empty? (ec2/describe-key-pairs :key-names [name])))
    (catch AmazonServiceException e
      false)))

(defn- create-key-pair []
  (when (key-pair-exists? "overlord")
    (throw (IllegalStateException. "Key pair overlord already exists")))
  (log/info "Creating key pair")
  (:key-material (ec2/create-key-pair :key-name "overlord")))

(defn- new-subnet [type availability-zone cidr-block]
  {:type type
   :availability-zone availability-zone
   :cidr-block cidr-block})

(defn generate-subnets [availability-zones [type cidr-block]]
  (let [cidr-blocks (split-cidr cidr-block 3)]
    (mapv (partial new-subnet type) availability-zones cidr-blocks)))

(defn generate-typed-cidr-blocks [cidr-block]
  (let [[public-shared private] (split-cidr cidr-block 1)
        [public shared] (split-cidr public-shared 1)]
    {:public public
     :shared shared
     :private private}))

(defn- update-network [{:keys [key-id access-key]} {:keys [region cidr-block] :as network}]
  (with-credential
    [key-id access-key region]
    (let [cidr-blocks (generate-typed-cidr-blocks cidr-block)
          availability-zones (list-availability-zones)
          generate-subnets-in-az (partial generate-subnets availability-zones)]
      (assoc network
             :private-key (create-key-pair)
             :subnets (apply concat (mapv generate-subnets-in-az cidr-blocks))))))

(defn prepare [name {:keys [key-id access-key networks] :as account}]
  (with-credential
    [key-id access-key]
    (assoc account
           :name name
           :networks (mapv (partial update-network account) networks)
           :aws-id (get-account-id))))