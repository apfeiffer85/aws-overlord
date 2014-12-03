(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [amazonica.core :refer [with-credential]]
            [aws-overlord.data.storage :as storage]
            [aws-overlord.tasks.security :as security]
            [aws-overlord.tasks.dns :as dns]
            [aws-overlord.tasks.networking :as networking]
            [aws-overlord.tasks.peering :as peering]))

(defmacro task [& body]
  `(try
     ~@body
     (catch Exception e#
       (log/error e# "Error in background thread"))))

(defn enforce [{:keys [storage]} {:keys [id key-id access-key networks] :as account}]
  (future
    (task
      (log/info "Enforcing settings in account..." (:name account))
      (with-credential [key-id access-key]
                       (log/info "Performing account-wide actions")
                       (security/run account)
                       (dns/run account))
      (doseq [{:keys [region] :as network} networks]
        (with-credential [key-id access-key region]
                         (log/info "Performing region-wide actions in" region)
                         (networking/run account network)
                         (let [all-accounts (storage/all-accounts storage)
                               existing-accounts (filterv (comp (complement #{id}) :id) all-accounts)]
                           (peering/run account network existing-accounts)))))))

(defrecord Enforcer [storage])

(defn ^Enforcer new-enforcer []
  (map->Enforcer {}))
