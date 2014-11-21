(ns aws-overlord.tasks.key-pair
  (:require [amazonica.aws.ec2 :as ec2]
            [aws-overlord.aws :refer [aws]]
            [clojure.tools.logging :as log]))

(defn- key-pair-exists? [credentials name]
  ; TODO compare fingerprint
  (not (empty? (aws ec2/describe-key-pairs credentials :key-names [name]))))

(defn ^:region-independent create-key-pair [{:keys [credentials] :as context}]
  (log/info "Enforcing key-pair on" (get-in context [:account :account/name]))
  (when (not (key-pair-exists? credentials "overlord"))
    (log/info "Key pair did not exist, creating...")
    (aws ec2/create-key-pair credentials :key-name "overlord")
    (log/info "Successfully created key pair."))
  ; TODO do we need the keypair id somewhere?
  context)
