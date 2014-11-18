(ns aws-overlord.schema
  (:require [datomic.api]))

(def account
  [{:db/id #db/id[:db.part/db]
    :db/ident :account/name ; TODO unique!
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "An account's name"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :account/key-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "An accounts's key id"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :account/access-key
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "An account's access key"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :account/networks
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "An account's networks"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :account/owner-email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "An account's owner's email"
    :db.install/_attribute :db.part/db}])

(def network
  [{:db/id #db/id[:db.part/db]
    :db/ident :network/region
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A network's region"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :network/vpc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A network's vpc"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :network/subnets
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A network's subnets"
    :db.install/_attribute :db.part/db}])

(def subnet
  [{:db/id #db/id[:db.part/db]
    :db/ident :subnet/availability-zone
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A subnet's availability zone"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :subnet/mask
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A network's network mask"
    :db.install/_attribute :db.part/db}])