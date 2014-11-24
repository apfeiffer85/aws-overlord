(ns aws-overlord.tasks.internet-gateway
  (:require [amazonica.aws.ec2 :as ec2]
            [aws-overlord.aws :refer [aws]]
            [aws-overlord.tasks.vpc :refer [create-vpc]]
            [clojure.tools.logging :as log]))

(defn- get-internet-gateway-id [credentials vpc-id]
  (-> (aws #'ec2/describe-internet-gateways credentials :filter {:attachment.vpc-id vpc-id}) first :internet-gateway-id))

(defn- create-and-attach-internet-gateway [credentials vpc-id]
  (let [internet-gateway-id (-> (aws #'ec2/create-internet-gateway credentials :internet-gateway-id))]
    (log/info "Created internet gateway" internet-gateway-id)
    (aws #'ec2/attach-internet-gateway :internet-gateway-id internet-gateway-id :vpc-id vpc-id)
    (log/info "Attached internet gateway" internet-gateway-id "to VPC" vpc-id)))

(defn ^:region-dependent ^{:requires [#'create-vpc]} attach-internet-gateway [{:keys [credentials network] :as context}]
  (let [{:keys [vpc-id]} network
        internet-gateway-id (get-internet-gateway-id credentials vpc-id)]
    (if internet-gateway-id
      (log/info "Found existing internet gateway" internet-gateway-id "for VPC" vpc-id)
      (create-and-attach-internet-gateway credentials vpc-id)))
  context)
