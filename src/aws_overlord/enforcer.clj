(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.scheduler :as s]))

(defn enforce-aws-settings [this]
  (log/info "Enforcing AWS settings..."))

(defrecord Enforcer [scheduler storage]
  component/Lifecycle

  (start [this]
    (s/schedule-with scheduler :function #(enforce-aws-settings this) :every 1000)
    this)

  (stop [this]
    this))

(defn ^Enforcer new-enforcer []
  (map->Enforcer {}))
