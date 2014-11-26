(ns aws-overlord.api.mapping
  (:require [datomic.api]
            [clojure.tools.logging :as log]))

(defn- subnet-to-db [subnet]
  (log/info "Mapping subnet" subnet "to db entity")
  {:subnet/type (get-in subnet [:type])
   :subnet/availability-zone (get-in subnet [:availability-zone])
   :subnet/cidr-block (get-in subnet [:cidr-block])})

(defn- subnet-from-db [subnet]
  (log/info "Mapping subnet" subnet "from db entity")
  {:type (get-in subnet [:subnet/type])
   :availability-zone (get-in subnet [:subnet/availability-zone])
   :cidr-block (get-in subnet [:subnet/cidr-block])})

(defn- network-to-db [network]
  (log/info "Mapping network" network "to db entity")
  {:network/region (get-in network [:region])
   :network/cidr-block (get-in network [:cidr-block])
   :network/subnets (mapv subnet-to-db (get-in network [:subnets]))})

(defn- network-from-db [network]
  (log/info "Mapping network" network "from db entity")
  {:region (get-in network [:network/region])
   :cidr-block (get-in network [:network/cidr-block])
   :subnets (sort-by (juxt :type :availability-zone) (mapv subnet-from-db (get-in network [:network/subnets])))})

(defn account-to-db [account]
  (log/info "Mapping account" account "to db entity")
  {:db/id #db/id[:db.part/user]
   :account/name (get-in account [:name])
   :account/account-id (get-in account [:account-id])
   :account/key-id (get-in account [:credentials :key-id])
   :account/access-key (get-in account [:credentials :access-key])
   :account/networks (mapv network-to-db (get-in account [:networks]))
   :account/owner-email (get-in account [:owner-email])})

(defn account-from-db [account]
  (log/info "Mapping account" account "from db entity")
  {:name (get-in account [:account/name])
   :id (get-in account [:account/account-id])
   :networks (map network-from-db (get-in account [:account/networks]))
   :owner-email (get-in account [:account/owner-email])})