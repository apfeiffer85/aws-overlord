(ns aws-overlord.tasks.dns
  (:import (java.util UUID))
  (:require [amazonica.aws.route53 :as route53]
            [clojure.tools.logging :as log]))

(defn- hosted-zone-exists? [name]
  (->> (route53/list-hosted-zones)
      :hosted-zones
      (map :name)
      (filter #{name})
      seq))

(defn- create-hosted-zone [name]
  (if-not (hosted-zone-exists? name)
    (do
      (log/info "Creating hosted zone" name)
      (route53/create-hosted-zone :name name
                                  :caller-reference (UUID/randomUUID)))
    (log/info "Hosted zone already exists")))

(defn run [{team-name :name}]
  (create-hosted-zone (str team-name ".aws.zalando."))
  (create-hosted-zone (str team-name ".aws.zalando.net.")))
