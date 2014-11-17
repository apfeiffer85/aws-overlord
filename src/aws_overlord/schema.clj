(ns aws-overlord.schema)

(def account
  [{:db/id #db/id[:db.part/db]
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
    :db.install/_attribute :db.part/db}])

(def team
  [{:db/id #db/id[:db.part/db]
    :db/ident :team/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A team's name"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :team/lead
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "A team's name"
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :team/members
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A team's members"
    :db.install/_attribute :db.part/db}])

(def member
  [{:db/id #db/id[:db.part/db]
    :db/ident :member/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A member's id"
    :db.install/_attribute :db.part/db}])

(def application
  [{:db/id #db/id[:db.part/db]
    :db/ident :application/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "An application's name"
    :db.install/_attribute :db.part/db}])