(ns aws-overlord.tasks.key-pair
  (:require [amazonica.aws.ec2 :as ec2]
            [aws-overlord.aws :refer [aws]]
            [clojure.tools.logging :as log]))

(defn- key-pair-exists? [name]
  ; TODO compare fingerprint
  (not (empty? (aws #'ec2/describe-key-pairs :key-names [name]))))

(defn ^:region-independent create-key-pair [context]
  (when (not (key-pair-exists? "overlord"))
    (log/info "Key pair did not exist, creating...")
    (aws #'ec2/create-key-pair :key-name "overlord")
    (log/info "Successfully created key pair.")))
