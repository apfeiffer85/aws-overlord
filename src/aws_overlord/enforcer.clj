(ns aws-overlord.enforcer
  (:require [clojure.tools.logging :as log]
            [amazonica.core :refer [with-credential]]
            [aws-overlord.data.storage :as storage]
            [aws-overlord.tasks.saml :as saml]
            [aws-overlord.tasks.dns :as dns]
            [aws-overlord.tasks.key-pair :as key-pair]
            [aws-overlord.tasks.stack :as stack]))

(defmacro task [& body]
  `(try
     ~@body
     (catch Exception e#
       (log/error "Error in background thread" e#))))

(defn enforce [{:keys [storage]} {:keys [key-id access-key networks] :as account}]
  (future
    (task
      (log/info "Enforcing settings in account..." (:name account))
      (with-credential [key-id access-key]
                       (log/info "Performing account-wide actions")
                       (saml/run account)
                       (dns/run account))
      (doseq [{:keys [region] :as network} networks]
        (with-credential [key-id access-key region]
                         (log/info "Performing region-wide actions in" region)
                         (when-let [private-key (key-pair/run)]
                           (storage/set-private-key storage network private-key))
                         (stack/create-stack "overlord" account network))))))

(defrecord Enforcer [storage])

(defn ^Enforcer new-enforcer []
  (map->Enforcer {}))
