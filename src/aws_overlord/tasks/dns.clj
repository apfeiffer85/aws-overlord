(ns aws-overlord.tasks.dns
  (:require [amazonica.aws.route53 :as route53]
            [clojure.tools.logging :as log]))

(defn- hosted-zone [name]
  (str name ".aws.zalando."))

(defn- hosted-zone-exists? [name]
  (->> (route53/list-hosted-zones)
      :hosted-zones
      (map :name)
      (filter #{(hosted-zone name)})
      seq))

(defn- create-hosted-zone [name]
  (if-not (hosted-zone-exists? name)
    (let [zone-name (hosted-zone name)]
      (log/info "Creating hosted zone" zone-name)
      (route53/create-hosted-zone :name zone-name
                                  :caller-reference name))
    (log/info "Hosted zone already exists")))

; TODO team.aws.zalando.
; TODO team.aws.zalando.net.
(defn run [{:keys [name]}]
  (create-hosted-zone name))
