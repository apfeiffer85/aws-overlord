(ns aws-overlord.aws
  (:require [clojure.tools.logging :as log]
            [amazonica.core :refer [set-root-unwrapping! with-credential]]))

(set-root-unwrapping! true)

(def ^:dynamic ^:private *key-id* nil)
(def ^:dynamic ^:private *access-key* nil)
(def ^:dynamic ^:private *region* nil)

(defn credentials []
  (if *region*
    [*key-id* *access-key* *region*]
    [*key-id* *access-key*]))

(defmacro switch-to [region & body]
  `(binding [*region* ~region]
     (log/info "Current region set to" ~region)
     (with-credential (credentials) ~@body)))

(defmacro login-to [account & body]
  `(binding [*key-id* (:key-id ~account)
             *access-key* (:access-key ~account)]
     (log/info "Current account set to" (:name ~account))
     (with-credential (credentials) ~@body)))