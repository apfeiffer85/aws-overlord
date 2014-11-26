(ns aws-overlord.logic.accounts
  (:require [aws-overlord.net :refer :all]
            [aws-overlord.api.mapping :refer :all]
            [clojure.string :refer [split]]
            [amazonica.aws.ec2 :as ec2]
            [amazonica.aws.identitymanagement :as iam]))

(defn- create-credentials [account]
  {:access-key (get-in account [:credentials :key-id])
   :secret-key (get-in account [:credentials :access-key])})

(defn- list-availability-zones [account region]
  (let [credentials (create-credentials account)]
    (mapv :zone-name (ec2/describe-availability-zones (assoc credentials :endpoint region)))))

(defn- get-account-id [account]
  (let [credentials (create-credentials account)]
    (nth (split (:arn (iam/get-user credentials)) #":") 4)))

(defn- new-subnet [type availability-zone cidr-block]
  {:type (name type)
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

(defn- update-network [account {:keys [region cidr-block] :as network}]
  (assoc network :subnets (apply concat (mapv (partial generate-subnets (list-availability-zones account region))
                                              (generate-typed-cidr-blocks cidr-block)))))

(defn prepare [{:keys [networks] :as account-data}]
  (let [account (assoc account-data :networks (mapv (partial update-network account-data) networks))
        account (assoc account :account-id (get-account-id account-data))
        db-account (account-to-db account)]
    db-account))