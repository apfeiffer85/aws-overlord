(ns aws-overlord.data.storage
  (:import (clojure.lang IPersistentVector)
           (java.sql PreparedStatement Connection Array ParameterMetaData))
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.sql :refer [delete where select update]]
            [clojure.walk :refer :all]
            [clojure.string :as string]))

(defrecord Storage [database])

(extend-protocol clojure.java.jdbc/ISQLParameter
  IPersistentVector
  (set-parameter [v ^PreparedStatement stmt ^long i]
    (let [conn (.getConnection stmt)
          meta (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta i)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt i (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt i v)))))

(extend-protocol clojure.java.jdbc/IResultSetReadColumn
  Array
  (result-set-read-column [val _ _]
    (into [] (.getArray val))))

(defn- rename [source target entity]
  (prewalk (fn [x] (if (map? x)
                     (into {} (map (fn [[k v]] [(keyword (string/replace (name k) source target)) v]) x))
                     x)) entity))

(def ^:private rename-inbound
  (partial rename "_" "-"))

(def ^:private rename-outbound
  (partial rename "-" "_"))

(defn- map-subnet-outbound [network-id subnet]
  (-> subnet
      (assoc :network-id network-id)
      (update-in [:type] name)
      (rename-outbound)))

(defn- map-network-outbound [account-id network]
  (-> network
      (dissoc :subnets)
      (assoc :account-id account-id)
      (rename-outbound)))

(defn- map-account-outbound [account]
  (-> account
      (dissoc :networks)
      (rename-outbound)))

(defn- map-subnet-inbound [subnet]
  (-> subnet
      (rename-inbound)
      (dissoc :network-id)
      (update-in [:type] keyword)))

(defn- map-network-inbound [network]
  (-> network
      (dissoc  :account-id)
      (rename-inbound)))

(defn- map-account-inbound [account]
  (-> account
      (rename-inbound)))

(def ^:private secret?
  #{:private_key
    :access_key})

(defn- obfuscate [entity]
  (let [f (fn [x] (if (map? x)
                        (into {} (map (fn [[k _ :as entry]]
                                        (if (secret? k) [k "<secret>"] entry)) x))
                        x))]
    (prewalk f (f entity))))

(defn- insert [database table entity]
  (log/info "Inserting into" table (obfuscate entity))
  (jdbc/insert! database table entity))

(defn insert-account [storage {:keys [networks] :as account}]
  (let [{:keys [database]} storage]
    (let [result (insert database :account (map-account-outbound account))
          account-id (-> result first :id)]
      (doseq [{:keys [subnets] :as network} networks]
        (let [result (insert database :network (map-network-outbound account-id network))
              network-id (-> result first :id)]
          (doseq [subnet subnets]
            (insert database :subnet (map-subnet-outbound network-id subnet))))))

    (log/info "Successfully inserted account")))

(defn delete-account [storage name]
  (let [{:keys [database]} storage]
    (log/info "Deleting account" name)
    (jdbc/execute! database (delete :account (where {:name name})))
    (log/info "Successfully deleted account" name)))

(defn- query-subnets [database network-id]
  (jdbc/query database
              (select * :subnet (where {:network_id network-id}))
              :row-fn map-subnet-inbound))

(defn- query-networks [database account-id]
  (jdbc/query database
              (select * :network (where {:account_id account-id}))
              :row-fn map-network-inbound))

(defn- select-account [database {account-id :id :as account}]
  (when-not (nil? account)
    (assoc account :networks
           (set (let [networks (query-networks database account-id)]
                  (map (fn [{network-id :id :as network}]
                         (assoc network :subnets (set (query-subnets database network-id))))
                       networks))))))

(defn account-by-name [storage name]
  (let [{:keys [database]} storage]
    (log/info "Fetching account" name)
    (select-account database (first (jdbc/query database (select * :account (where {:name name}))
                                                :row-fn map-account-inbound)))))

(defn all-accounts [storage]
  (let [{:keys [database]} storage]
    (log/info "Fetching all accounts")
    (map (partial select-account database) (jdbc/query database (select * :account)
                                                       :row-fn map-account-inbound))))

(defn ^Storage new-storage [{:keys [protocol host port name user password]
                             :or {protocol "postgresql"
                                  host "localhost"
                                  port 5432
                                  name "overlord"
                                  user "postgres"
                                  password "postgres"}}]
  (map->Storage {:database {:subprotocol protocol
                            :subname (str "//" host ":" port "/" name)
                            :user user
                            :password password}}))