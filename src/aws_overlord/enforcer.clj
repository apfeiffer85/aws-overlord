(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.scheduler :as s]
            [aws-overlord.aws :as aws]))

(defn enforce-aws-settings [this]
  (log/info "Starting new enforcer run...")
  (doseq [account [(:aws-settings this)]]
    ; for every aws account to manage...

    ; TODO check for configured IAM groups, roles and policies (admin, owner)
    ; TODO check that overlord and owner user are in their special groups (admin, owner)
    ; TODO check that no other users are in the special groups
    ; TODO check that users are only in groups and have no direct policies

    ; TODO check setup of VPC
    ; TODO check setup of subnets
    ; TODO check setup for NAT/VPN?

    (doseq [user (aws/list-users account)]
      (log/info "User" (:user-name user) "groups:"
                (map :group-name (aws/list-groups-for-user account (:user-name user)))))))

(defrecord Enforcer [scheduler storage aws-settings]
  component/Lifecycle

  (start [this]
    (s/schedule-with scheduler :function #(enforce-aws-settings this) :every 10000)
    this)

  (stop [this]
    this))

(defn ^Enforcer new-enforcer [aws-settings]
  (map->Enforcer {:aws-settings aws-settings}))
