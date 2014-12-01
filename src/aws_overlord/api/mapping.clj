(ns aws-overlord.api.mapping
  (:require [clojure.tools.logging :as log]))

(defn- subnet-to-db [subnet]
  (log/info "Mapping subnet" subnet "to db entity")
  {:type (get-in subnet [:type])
   :availability-zone (get-in subnet [:availability-zone])
   :cidr-block (get-in subnet [:cidr-block])})

(defn- subnet-from-db [subnet]
  (log/info "Mapping subnet" subnet "from db entity")
  {:type (get-in subnet [:type])
   :availability-zone (get-in subnet [:availability-zone])
   :cidr-block (get-in subnet [:cidr-block])})

(defn- network-to-db [network]
  (log/info "Mapping network" network "to db entity")
  {:region (get-in network [:region])
   :cidr-block (get-in network [:cidr-block])
   :subnets (mapv subnet-to-db (get-in network [:subnets]))})

(defn- network-from-db [network]
  (log/info "Mapping network" network "from db entity")
  {:region (get-in network [:region])
   :cidr-block (get-in network [:cidr-block])
   :subnets (sort-by (juxt :type :availability-zone) (mapv subnet-from-db (get-in network [:subnets])))})

(defn account-to-db [account]
  (log/info "Mapping account" account "to db entity")
  {:name (get-in account [:name])
   :aws-id (get-in account [:aws-id])
   :key-id (get-in account [:credentials :key-id])
   :access-key (get-in account [:credentials :access-key])
   :networks (mapv network-to-db (get-in account [:networks]))
   :owner-email (get-in account [:owner-email])})

(defn account-from-db [account]
  (log/info "Mapping account" account "from db entity")
  {:name (get-in account [:name])
   :aws-id (get-in account [:aws-id])
   :networks (map network-from-db (get-in account [:networks]))
   :owner-email (get-in account [:owner-email])})