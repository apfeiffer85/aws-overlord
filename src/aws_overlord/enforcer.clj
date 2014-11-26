(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.scheduler :as s]
            [aws-overlord.tasks.key-pair :refer :all]))

(def ^:private tasks
  [#'create-key-pair])

(defn- region-dependent? [task]
  (:region-dependent (meta task)))

(defn- category-of [task]
  (if (region-dependent? task) :region-dependent :region-independent))

(defn- grouped-tasks [tasks]
  (group-by category-of tasks))

(defn- pipe [fs]
  (apply comp (reverse fs)))

(defn- region-independent-tasks [tasks]
  (pipe (:region-independent tasks)))

(defn- instrument [{:keys [network/cidr-block] :as network} task]
  (fn [context]
    (let [network-context (assoc context :network network)]
      (task network-context))))

(defn- region-dependent-tasks [tasks networks]
  (for [{:keys [network/cidr-block] :as network} networks]
    (instrument network (pipe (:region-dependent tasks)))))

(defn enforce-account [account]
  (log/info "Enforcing settings in account..." (:account/name account))
  (let [context {:credentials {:access-key (:account/key-id account)
                               :secret-key (:account/access-key account)}
                 :account account}
        tasks (grouped-tasks tasks)
        networks (:account/networks account)
        task (pipe [(region-independent-tasks tasks)
                    (region-dependent-tasks tasks networks)])]
    (task context)))

(defn enforce-accounts [accounts]
  ; TODO parallel?
  ; TODO save persistent context information back to storage
  (doseq [account accounts]
    (enforce-account account)))

(defn- run [enforcer]
  (log/info "Starting new enforcer run...")
  ; TODO extract all accounts from storage
  (enforce-accounts []))

(defrecord Enforcer [scheduler storage aws-settings]
  component/Lifecycle

  (start [this]
    (s/schedule-with scheduler :function #(run this) :every 10000)
    this)

  (stop [this]
    this))

(defn ^Enforcer new-enforcer [aws-settings]
  (map->Enforcer {:aws-settings aws-settings}))
