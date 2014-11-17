(ns aws-overlord.storage
  (:require [com.stuartsierra.component :as component]))

(defrecord Storage [url connection]
  component/Lifecycle

  (start [this]
    (if connection
      this
      (do
        this)))

  (stop [this]
    this))

(defn ^Storage new-storage [url]
  (map->Storage {:url url}))