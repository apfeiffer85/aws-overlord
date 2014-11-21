(ns aws-overlord.storage-test
  (:import (java.util UUID))
  (:require [clojure.test :refer :all]
            [aws-overlord.data.storage :refer :all]
            [com.stuartsierra.component :refer :all]
            [datomic.api :as d]
            [clojure.walk :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]))

(defn- new-subnet [region zone range block]
  {:subnet/availability-zone (str region zone)
   :subnet/cidr-block (str "10." range "." block ".0/17")})

(defn- new-network [region range]
  {:network/region region
   :network/cidr-block (str "10." range ".0.0/19")
   :network/subnets #{(new-subnet region "a" range 1)
                      (new-subnet region "b" range 2)
                      (new-subnet region "c" range 3)}})

(defn- new-account [name]
  {:db/id #db/id[:db.part/user]
   :account/name name
   :account/key-id "key-id"
   :account/access-key "access-key"
   :account/networks #{(new-network "eu-west-1" 17)
                       (new-network "eu-central-1" 18)}
   :account/owner-email "d.fault@example.com"})

(defn- without-id [entity]
  (dissoc entity :db/id))

(def ^:dynamic *unit*)

(defn- with-db [test]
  (let [url (str "datomic:mem://" (UUID/randomUUID))
        system (start-system {:storage (new-storage url)})]
    (binding [*unit* (:storage system)]
      (test))
    (stop-system system)
    (log/info "Deleting database" url)
    (d/delete-database url)))

(use-fixtures :each with-db)

(deftest test-insert-account
  (insert-account *unit* (new-account "foo"))
  (is (not (nil? (account-by-name *unit* "foo")))))

(deftest test-account-by-name-missing
  (is (nil? (account-by-name *unit* "foo"))))

(deftest test-account-by-name-present
  (let [account (new-account "foo")]
    (insert-account *unit* account)
    (let [actual (account-by-name *unit* "foo")]
      (is (= (without-id actual) (without-id account))))))

(deftest test-delete-account
  (insert-account *unit* (new-account "foo"))
  (is (not (nil? (account-by-name *unit* "foo"))))
  (delete-account *unit* "foo")
  (is (nil? (account-by-name *unit* "foo"))))

(deftest test-all-accounts
  (let [first (new-account "foo")
        second (new-account "bar")]
    (insert-account *unit* first)
    (insert-account *unit* second)
    (let [accounts (map without-id (all-accounts *unit*))]
      (is (= 2 (count accounts)))
      (is (some #{(without-id first)} accounts))
      (is (some #{(without-id second)} accounts)))))
