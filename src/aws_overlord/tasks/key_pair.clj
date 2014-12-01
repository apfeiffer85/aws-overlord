(ns aws-overlord.tasks.key-pair
  (:import (com.amazonaws AmazonServiceException))
  (:require [amazonica.aws.ec2 :as ec2]
            [clojure.tools.logging :as log]))

(defn- key-pair-exists? [name]
  ; TODO compare fingerprint
  (try
    (not (empty? (ec2/describe-key-pairs :key-names [name])))
    (catch AmazonServiceException e
      false)))

(defn run []
  (if-not (key-pair-exists? "overlord")
    (do
      (log/info "Creating key pair")
      (:key-material (ec2/create-key-pair :key-name "overlord")))
    (log/info "Key pair already exists")))
