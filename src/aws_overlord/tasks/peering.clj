(ns aws-overlord.tasks.peering
  (:require [amazonica.aws.ec2 :as ec2]
            [amazonica.core :refer [with-credential]]
            [clojure.tools.logging :as log]))

(defn- filter-pair [key value]
  {:name key
   :value value})

(defn- directly-peered? [acceptor-id acceptor-cidr requester-id requester-cidr]
  (boolean (ec2/describe-vpc-peering-connections
             :filters [(filter-pair "accepter-vpc-info.cidr-block" acceptor-cidr)
                       (filter-pair "accepter-vpc-info.owner-id" acceptor-id)
                       (filter-pair "requester-vpc-info.cidr-block" requester-cidr)
                       (filter-pair "requester-vpc-info.owner-id" requester-id)])))

(defn- peered? [left-id left-cidr right-id right-cidr]
  (or (directly-peered? left-id left-cidr right-id right-cidr)
      (directly-peered? right-id right-cidr left-id left-cidr)))

(defn- fail-if [value pred & msg]
  (if (pred value)
    (throw (IllegalStateException. (with-out-str (println msg))))
    value))

(defn- find-vpc-id [cidr-block]
  (-> (ec2/describe-vpcs :filters [(filter-pair "cidr" cidr-block)])
      first
      :vpc-id
      (fail-if nil? "No VPC for given CIDR" cidr-block)))

(defn- find-route-tables []
  (mapv :route-table-id (ec2/describe-route-tables :filters [(filter-pair "tag:VPC-Routing" "true")])))

(defn- route [vpc-peering-connection-id cidr-block]
  (doseq [route-table-id (find-route-tables)]
    (ec2/create-route :route-table-id route-table-id
                      :destination-cidr-block cidr-block
                      :vpc-peering-connection-id vpc-peering-connection-id)))

(defn- peer [{new-cidr-block :cidr-block :keys [region]}
             {:keys [aws-id key-id access-key]} {existing-cidr-block :cidr-block}]
  (let [vpc-id (find-vpc-id new-cidr-block)
        peer-vpc-id (with-credential [key-id access-key region] (find-vpc-id existing-cidr-block))
        connection-id (-> (ec2/create-vpc-peering-connection :vpc-id vpc-id
                                                             :peer-vpc-id peer-vpc-id
                                                             :peer-owner-id aws-id)
                          :vpc-peering-connection-id)]

    (with-credential [key-id access-key region]
                     (ec2/accept-vpc-peering-connection :vpc-peering-connection-id connection-id)
                     (route connection-id new-cidr-block))

    (route connection-id existing-cidr-block)))


(defn- find-network-by-region [networks region]
  (-> networks (filter (comp #{region} :region)) first))

(defn run [{new-id :aws-id new-name :name}
           {new-cidr-block :cidr-block :keys [region] :as new-network}
           existing-accounts]
  (when (empty? existing-accounts)
    (log/info "No existing accounts, nothing to peer with."))

  (doseq [{existing-id :aws-id
           existing-name :name
           existing-networks :networks
           :as existing-account} existing-accounts]
    (when-let [{existing-cidr-block :cidr-block :as existing-network} (find-network-by-region existing-networks region)]
      (if-not (peered? new-id new-cidr-block existing-id existing-cidr-block)
        (do
          (log/info "Peering" new-name "and" existing-name)
          (peer new-network existing-account existing-network))
        (log/info new-name "and" existing-name "are already peered")))))
