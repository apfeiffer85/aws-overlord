(ns aws-overlord.mapping
  (:require [clojure.tools.logging :as log]))

(defn account-to-db [account]
  (log/info "Mapping account" account "to db entity")
  {:db/id #db/id[:db.part/user]
   :account/name (get-in account [:name])
   :account/key-id (get-in account [:credentials :key-id])
   :account/access-key (get-in account [:credentials :access-key])
   :account/owner-email (get-in account [:owner-email])})

(defn account-from-db [account]
  (log/info "Mapping account" account "from db entity")
  {:name (get-in account [:account/name])
   :networks [] ; TODO fill?
   :owner-email (get-in account [:account/owner-email])})