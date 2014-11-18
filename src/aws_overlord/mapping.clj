(ns aws-overlord.mapping
  (:require [clojure.tools.logging :as log]))

(defn- subnet-to-db [subnet]
  (log/info "Mapping subnet" subnet "to db entity")
  {:subnet/availability-zone (get-in subnet [:availability-zone])
   :subnet/mask (get-in subnet [:mask])})

(defn- subnet-from-db [subnet]
  (log/info "Mapping subnet" subnet "from db entity")
  {:availability-zone (get-in subnet [:subnet/availability-zone])
   :mask (get-in subnet [:subnet/mask])})

(defn- network-to-db [network]
  (log/info "Mapping network" network "to db entity")
  {:network/region (get-in network [:region])
   :network/vpc (get-in network [:vpc])
   :network/subnets (map subnet-to-db (get-in network [:subnets]))})

(defn- network-from-db [network]
  (log/info "Mapping network" network "from db entity")
  {:region (get-in network [:network/region])
   :vpc (get-in network [:network/vpc])
   :subnets (map subnet-from-db (get-in network [:network/subnets]))})

(defn account-to-db [account]
  (log/info "Mapping account" account "to db entity")
  {:db/id #db/id[:db.part/user]
   :account/name (get-in account [:name])
   :account/key-id (get-in account [:credentials :key-id])
   :account/access-key (get-in account [:credentials :access-key])
   :account/networks (map network-to-db (get-in account [:networks]))
   :account/owner-email (get-in account [:owner-email])})

(defn account-from-db [account]
  (log/info "Mapping account" account "from db entity")
  {:name (get-in account [:account/name])
   :networks (map network-from-db (get-in account [:account/networks]))
   :owner-email (get-in account [:account/owner-email])})