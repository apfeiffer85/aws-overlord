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

(defmacro login-to [{:keys [name key-id access-key]} & body]
  `(binding [*key-id* ~key-id
             *access-key* ~access-key]
     (log/info "Current account set to" ~name)
     (with-credential (credentials) ~@body)))