(ns aws-overlord.data.storage-test
  (:require [clojure.test :refer :all]
            [aws-overlord.data.storage :refer :all]
            [aws-overlord.config :as config]
            [environ.core :refer [env]]
            [clojure.walk :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

(defn- new-subnet [region zone range block]
  {:type              :public
   :availability-zone (str region zone)
   :cidr-block        (str "10." range "." block ".0/17")})

(defn- new-network [region range]
  {:region         region
   :cidr-block     (str "10." range ".0.0/19")
   :vpn-gateway-ip (str "20." range ".0.1")
   :vpn-routes     ["10.10.0.0/19"
                    "10.120.0.0/21"
                    "10.200.0.0/20"]
   :name-servers   ["1.2.3.4"]
   :private-key    "s3cr3t"
   :subnets        #{(new-subnet region "a" range 1)
                     (new-subnet region "b" range 2)
                     (new-subnet region "c" range 3)}})

(defn- new-account [name]
  {:name       name
   :aws-id     (str (rand-int 1000))
   :key-id     "key-id"
   :access-key "access-key"
   :networks   #{(new-network "eu-west-1" 17)
                 (new-network "eu-central-1" 18)}})

(defn- without-id [entity]
  (let [remove-id #(if (map? %) (dissoc % :id :account-id :network-id) %)]
    (prewalk remove-id (remove-id entity))))

(def ^:dynamic *unit*)

(defn- with-db [test]
  (let [{config :db} (config/parse env ["db"])
        storage (new-storage config)
        database (:database storage)]
    (jdbc/with-db-transaction
      [connection database]
      (binding [*unit* (assoc storage :database connection)]
        (jdbc/db-set-rollback-only! connection)
        (test)))))

(use-fixtures :each with-db)

(deftest test-insert-account
  (insert-account *unit* (new-account "foo"))
  (is (not (nil? (account-by-name *unit* "foo")))))

(deftest test-account-by-name-missing
  (is (nil? (account-by-name *unit* "foo"))))

(deftest test-account-by-name-present
  (let [account (new-account "foo")]
    (insert-account *unit* account)
    (let [actual (without-id (account-by-name *unit* "foo"))]
      (is (= actual account)))))

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
