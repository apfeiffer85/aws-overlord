(ns aws-overlord.storage
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.schema :as schema]
            [datomic.api :as d :refer [db q]]))

(defrecord Storage [url connection]
  component/Lifecycle

  (start [this]
    (if connection
      this
      (do
        (log/info "Creating database (if needed)")
        (let [new (d/create-database url)]
          (when new
            (log/info "Created database" url))
          (let [connection (d/connect url)]
            (when new
              (log/info "Creating schema")
              (deref (d/transact connection (concat schema/account schema/network schema/subnet))))
            (assoc this :connection connection))))))

  (stop [this]
    (dissoc this :connection)))

(defn insert-account [storage account]
  (let [{:keys [connection]} storage]
    (log/info "Inserting account" account)
    (deref (d/transact connection [account]))
    (log/info "Successfully inserted account" account)))

(defn delete-account [storage name]
  (let [{:keys [connection]} storage]
    (log/info "Deleting account" name)
    (deref (d/transact connection [[:db.fn/retractEntity [:account/name name]]]))
    (log/info "Successfully deleted account" name)))

(defn account-by-name [storage name]
  (let [{:keys [connection]} storage
        db (db connection)]
    (log/info "Fetching account" name)
    (let [entity-id (q '[:find ?account .
                         :in $ ?name
                         :where [?account :account/name ?name]]
                       db
                       name)]
      (if entity-id
        (d/entity db entity-id)
        (log/info "Unable to find entity with id" entity-id)))))

(defn ^Storage new-storage [url]
  (map->Storage {:url url}))