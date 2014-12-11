(ns aws-overlord.tasks.dns
  (:import (java.util UUID))
  (:require [aws-overlord.aws :refer :all]
            [aws-overlord.config :as config]
            [amazonica.aws.route53 :as route53]
            [clojure.tools.logging :as log]
            [clojure.string :refer [split]]))

(defn- hosted-zone-exists? [name]
  (->> (route53/list-hosted-zones)
       :hosted-zones
       (map :name)
       (filter #{name})
       seq))

(defn- prepare-change-batch [name name-servers]
  {:changes [{:action "CREATE"
              :resource-record-set {:type "NS"
                                    :name name
                                    :resource-records (mapv #(hash-map :value %) name-servers)
                                    :ttl 300}}]})

(defn- find-hosted-zone-id [domain]
  (->> (route53/list-hosted-zones)
       :hosted-zones
       (filter (comp #{domain} :name))
       (map :id)
       first))

(defn change-resource-record-sets [dns-account base-domain domain name-servers]
  (if (empty? dns-account)
    (log/info "No DNS account specified, resource record sets won't be created for" base-domain)
    (login-to (assoc dns-account :name "aws")
              (if-let [hosted-zone-id (find-hosted-zone-id base-domain)]
                (do
                  (log/info "Adding resource records for" domain "to" base-domain)
                  (route53/change-resource-record-sets :hosted-zone-id hosted-zone-id
                                                       :change-batch (prepare-change-batch domain name-servers)))
                (log/info "Hosted zone" base-domain "does not exist")))))

(defn- create-hosted-zone [domain]
  (if-not (hosted-zone-exists? domain)
    (do
      (log/info "Creating hosted zone" domain)
      (-> (route53/create-hosted-zone :name domain :caller-reference (UUID/randomUUID))
          :delegation-set
          :name-servers))
    (log/info "Hosted zone" domain "already exists")))

(defn run [{team-name :name} {:keys [domains] :as config}]
  (let [{dns-account :account} (config/parse config [:account])]
    (if domains
      (doseq [base-domain (split domains #",")]
        (let [domain (str team-name "." base-domain)]
          (log/info "Setting up" domain)
          (when-let [name-servers (create-hosted-zone domain)]
            (change-resource-record-sets dns-account base-domain domain name-servers))))
      (log/info "No domains given via dns.domains"))))
