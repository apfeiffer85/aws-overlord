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
  (d/transact (:connection storage) [(assoc entity :db/id -1)]))

(defn account-by-name [storage name]
  (let [connection (:connection storage)
        db (db connection)]
    (log/info "Database is" db)
    (first (q '[:find ?account
                :in $ ?name
                :where [?account :account/name ?name]]
              db
              name))))

(defn ^Storage new-storage [url]
  (map->Storage {:url url}))