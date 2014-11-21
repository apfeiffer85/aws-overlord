(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [aws-overlord.scheduler :as s]
            [aws-overlord.tasks.key-pair :refer :all]
            [aws-overlord.tasks.vpc :refer :all]
            [aws-overlord.tasks.subnet :refer :all]))

(def ^:private tasks
  ['aws-overlord.tasks.key-pair/create-key-pair
   'aws-overlord.tasks.vpc/create-vpcs
   'aws-overlord.tasks.subnet/create-subnets])

(defn- region-dependent? [task]
  (:region-dependent (meta (resolve task))))

(defn- category-of [task]
  (if (region-dependent? task) :region-dependent :region-independent))

(defn- grouped-tasks [tasks]
  (into {} (map (fn [[category tasks]]
                  [category (map resolve tasks)])
                (group-by category-of tasks))))

(defn- pipe [fs]
  (apply comp (reverse fs)))

(defn- region-independent-tasks [tasks]
  (pipe (:region-independent tasks)))

(defn- index-by [coll attr val]
  (first (keep-indexed (fn [i el] (when #(= val (attr el)) i)) coll)))

(defn- region-dependent-tasks [tasks networks]
  ; TODO we need to propagate the changes made to :network back to the original under :account :account/networks
  (pipe (for [task (:region-dependent tasks)
              {:keys [network/cidr-block] :as network} networks]
          (fn [context]
            (let [result (task (merge context {:network network}))]
              (-> result
                  (update-in [:account :account/networks (index-by networks :network/cidr-block cidr-block)] merge (:network result))
                  (dissoc :network)))))))

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
