(ns aws-overlord.logic.accounts
  (:import (com.amazonaws AmazonServiceException))
  (:require [aws-overlord.aws :refer :all]
            [aws-overlord.net :refer :all]
            [aws-overlord.tasks.security :as security]
            [aws-overlord.tasks.dns :as dns]
            [aws-overlord.tasks.networking :as networking]
            [aws-overlord.tasks.peering :as peering]
            [clojure.tools.logging :as log]
            [clojure.string :refer [split]]
            [amazonica.aws.ec2 :as ec2]
            [amazonica.aws.identitymanagement :as iam]))

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
  (let [key-pair-name "overlord"]
    (when (key-pair-exists? key-pair-name)
      (throw (IllegalStateException. (str "Key pair " key-pair-name " already exists"))))
    (log/info "Creating key pair")
    (:key-material (ec2/create-key-pair :key-name key-pair-name))))

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

(defn- update-network [{:keys [region cidr-block] :as network}]
  (switch-to region
             (let [cidr-blocks (generate-typed-cidr-blocks cidr-block)
                   availability-zones (list-availability-zones)
                   generate-subnets-in-az (partial generate-subnets availability-zones)]
               (assoc network
                      :private-key (create-key-pair)
                      :subnets (set (apply concat (mapv generate-subnets-in-az cidr-blocks)))))))

(defn prepare [name {:keys [networks] :as account}]
  (let [account (assoc account :name name)]
    (login-to account
              (assoc account
                     :networks (mapv update-network networks)
                     :aws-id (get-account-id)))))

(defmacro background [& body]
  `(future
     (try
       ~@body
       (catch Exception e#
         (log/error e# "Error in background thread")))))

(defn configure [{:keys [networks] :as account} existing-accounts {:keys [dns]}]
  (background
    (log/info "Configuring account" (:name account))
    (login-to account
              (log/info "Performing account-wide actions")
              (security/run account)
              (dns/run account dns)
              (doseq [{:keys [region] :as network} networks]
                (switch-to region
                           (log/info "Performing region-wide actions in" region)
                           (networking/run account network)
                           (peering/run account network existing-accounts))))))