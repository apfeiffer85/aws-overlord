(ns aws-overlord.datomic
  (:require [datomic.api :as d]
            [clojure.walk :refer :all]))

(defn- entity-map? [e]
  (instance? datomic.query.EntityMap e))

(defn- touch [x]
  (if (entity-map? x) (into {} (d/touch x)) x))

(defn touch-recursively [entity]
  (prewalk touch (touch entity)))