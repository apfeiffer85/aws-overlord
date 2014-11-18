(ns aws-overlord.aws
  (:require [amazonica.core :as amazonica]
            [amazonica.aws.identitymanagement :as iam]))

(amazonica/set-root-unwrapping! true)

(defn list-users [settings]
  (iam/list-users settings))

(defn list-access-keys [settings user-name]
  (iam/list-access-keys settings :user-name user-name))
