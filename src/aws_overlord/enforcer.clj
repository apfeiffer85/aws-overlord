(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.scheduler :as s]
            [aws-overlord.aws :as aws]))

(defn enforce-aws-settings [this]
  (log/info "Starting new enforcer run...")
  (doseq [account [(:aws-settings this)]]
    (doseq [user (aws/list-users account)]
      (let [user-name (:user-name user)
            keys (aws/list-access-keys account user-name)]
        (log/info "User" user-name "keys:" keys)))))


(defrecord Enforcer [scheduler storage aws-settings]
  component/Lifecycle

  (start [this]
    (s/schedule-with scheduler :function #(enforce-aws-settings this) :every 10000)
    this)

  (stop [this]
    this))

(defn ^Enforcer new-enforcer [aws-settings]
  (map->Enforcer {:aws-settings aws-settings}))
