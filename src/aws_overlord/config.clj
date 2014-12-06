(ns aws-overlord.config
  (:require [clojure.tools.logging :as log]
            [clojure.string :refer [replace-first]]))

(defn- strip [namespace k]
  (keyword (replace-first (name k) (str namespace "-") "")))

(defn- namespaced [config namespace]
  (into {} (map (fn [[k v]] [(strip namespace k) v])
                (filter (fn [[k v]]
                          (.startsWith (name k) (str namespace "-")))
                        config))))

(defn parse [config namespaces]
  (let [namespaced-configs (into {} (map (juxt keyword (partial namespaced config)) namespaces))]
    (doseq [[namespace namespaced-config] namespaced-configs]
      (log/info "Configuring" namespace "with" namespaced-config))
    namespaced-configs))
