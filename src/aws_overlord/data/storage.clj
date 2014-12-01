(ns aws-overlord.data.storage
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.data.schema :as schema]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.sql :refer [delete where select update]]
            [clojure.walk :refer :all]
            [clojure.string :as string]))

(defrecord Storage [database]
  component/Lifecycle

  (start [this]
    (log/info "Performing database setup")
    (doseq [create-table [schema/account schema/network schema/subnet]]
      (jdbc/execute! database [create-table]))
    this)

  (stop [this]
    this))

(defn- rename [source target entity]
  (prewalk (fn [x] (if (map? x)
                     (into {} (map (fn [[k v]] [(keyword (string/replace (name k) source target)) v]) x))
                     x)) entity))

(defn- insert [database table entity]
  (jdbc/insert! database table (rename "-" "_" entity)))

(defn insert-account [storage {:keys [networks] :as account}]
  (let [{:keys [database]} storage]
    (log/info "Inserting account" account)
    (let [result (insert database :account (dissoc account :networks))
          account-id (-> result first :id)]
      (log/warn "Got from db" result)
      (doseq [{:keys [subnets] :as network} networks]
        (let [result (insert database :network (assoc (dissoc network :subnets) :account-id account-id))
              network-id (-> result first :id)]
          (log/warn "Got from db" result)
          (doseq [subnet subnets]
            (insert database :subnet (update-in (assoc subnet :network-id network-id) [:type] name))))))

    (log/info "Successfully inserted account" account)))

(defn set-private-key [storage {:keys [id]} private-key]
  (log/info "Saving private key")
  (let [{:keys [database]} storage]
    (jdbc/execute! database (update :network
                                    {:private_key private-key}
                                    (where {:id id})))))

(defn delete-account [storage name]
  (let [{:keys [database]} storage]
    (log/info "Deleting account" name)
    (jdbc/execute! database (delete :account
                                    (where {:name name})))
    (log/info "Successfully deleted account" name)))

(defn- select-account [database {account-id :id :as account}]
  (when-not (nil? account)
    (assoc account :networks
           (set (map (fn [{network-id :id :as network}]
                       (assoc network :subnets
                              (set (map #(update-in % [:type] keyword)
                                        (jdbc/query database (select * :subnet (where {:network_id network-id}))
                                                    :row-fn (partial rename "_" "-"))))))
                     (jdbc/query database (select * :network (where {:account_id account-id}))
                                 :row-fn (partial rename "_" "-")))))))

(defn account-by-name [storage name]
  (let [{:keys [database]} storage]
    (log/info "Fetching account" name)
    (select-account database (first (jdbc/query database (select * :account (where {:name name}))
                                                :row-fn (partial rename "_" "-"))))))

(defn all-accounts [storage]
  (let [{:keys [database]} storage]
    (map (partial select-account database) (jdbc/query database (select * :account)
                                                       :row-fn (partial rename "_" "-")))))

(defn ^Storage new-storage [database]
  (map->Storage {:database database}))