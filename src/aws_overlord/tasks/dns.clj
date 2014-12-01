(ns aws-overlord.tasks.dns
  (:require [amazonica.aws.route53 :as route53]
            [clojure.tools.logging :as log]))

(defn- hosted-zone [name]
  (str name ".aws.zalando."))

; TODO is broken somehow, always returns true
(defn- hosted-zone-exists? [name]
  (boolean (filter (comp #{(hosted-zone name)} :name)
                   (route53/list-hosted-zones))))

(defn- create-hosted-zone [name]
  (if-not (hosted-zone-exists? name)
    (let [zone-name (hosted-zone name)]
      (log/info "Creating hosted zone" zone-name)
      (route53/create-hosted-zone :name zone-name
                                  :caller-reference name))
    (log/info "Hosted zone already exists")))

(defn run [{:keys [name]}]
  (create-hosted-zone name))
