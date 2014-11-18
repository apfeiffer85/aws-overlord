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
              (d/transact connection (concat schema/account schema/network)))
            (assoc this :connection connection))))))

  (stop [this]
    (dissoc this :connection)))

(defn insert [storage entity]
  (let [{:keys [connection]} storage]
    (log/info "Inserting entity" entity)
    (d/transact connection [entity])
    (log/info "Successfully inserted entity" entity)))

(defn account-by-name [storage name]
  (let [{:keys [connection]} storage
        db (db connection)]
    (log/info "Fetching account" name)
    (let [entity-id (q '[:find ?account .
                         :in $ ?name
                         :where [?account :account/name ?name]]
                       db
                       name)]
      (when entity-id
        (d/entity db entity-id)))))

(defn ^Storage new-storage [url]
  (map->Storage {:url url}))