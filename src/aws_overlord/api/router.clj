(ns aws-overlord.api.router
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :refer :all]
            [compojure.api.routes :as routes]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [aws-overlord.data.storage :refer [insert-account delete-account account-by-name]]
            [aws-overlord.logic.accounts :as accounts]
            [aws-overlord.api.mapping :refer :all]
            [aws-overlord.net :refer :all]))

(def ^:private opt s/optional-key)

(s/defschema AccountView
             {:name String
              :networks [{:region String
                          :cidr-block String
                          :subnets [{:type String
                                     :availability-zone String
                                     :cidr-block String}]}]
              :owner-email String})

(s/defschema AccountCreation
             {:credentials {:key-id String
                            :access-key String}
              :networks [{:region String
                          :cidr-block String}]
              :owner-email String})

(defrecord Router [storage])

(defn- api-routes [router]
  (routes/with-routes

    (swaggered
      "Accounts"
      :description "Account management operations"

      (POST*
        "/accounts/:name" []
        :summary "Configures an account"
        :path-params [name :- String]
        :return AccountView
        :body [account AccountCreation]
        (log/info "Configuring account" name)
        (let [{:keys [storage]} router
              named-account (assoc account :name name)]
          (insert-account storage (accounts/prepare named-account))
          {:status 202
           :body (account-from-db (account-by-name storage name))}))

      (GET*
        "/accounts/" []
        :summary "Retrieves all accounts"
        :return [AccountView]
        (log/info "Retrieving all accounts")
        {:status 200 :body []})

      (GET*
        "/accounts/:name" []
        :summary "Retrieves an account"
        :path-params [name :- String]
        :return AccountView
        (log/info "Retrieving account" name)
        (let [{:keys [storage]} router
              account (account-by-name storage name)]
          (if account
            {:status 200 :body (account-from-db account)}
            {:status 404})))

      (GET*
        "/accounts/:name/status" []
        :summary "Retrieves an account's status"
        :path-params [name :- String]
        :return AccountView
        (log/info "Retrieving status of account" name)
        (let [{:keys [storage]} router
              account (account-by-name storage name)]
          (if account
            {:status 200 :body nil}
            {:status 404}))))

    (swaggered
      "Access"
      :description "Access management operations"

      (POST*
        "/access/:instance-id/:user" []
        :summary "Grants the given user access to the specified instance"
        :path-params [instance-id :- String
                      user :- String]
        (log/info "Granting" user "access to" instance-id))

      (GET*
        "/access" []
        :summary "Retrieves all current access information"
        (log/info "Retrieving all access information"))

      (GET*
        "/access/:instance-id" []
        :summary "Retrieves access information for the given instance"
        :path-params [instance-id :- String]
        (log/info "Retrieving access information for instance" instance-id))

      (GET*
        "/access/:instance-id/:user" []
        :summary "Retrieves access information for the given user on the specified instance"
        :path-params [instance-id :- String
                      user :- String]
        (log/info "Retrieving access information for" user "on instance" instance-id))

      (DELETE*
        "/access/:instance-id/:user" []
        :summary "Revokes the given user access to the specified instance"
        :path-params [instance-id :- String
                      user :- String]
        (log/info "Revokes" user "access to" instance-id)))))

(defn- exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Caught exception in web-tier")
        (throw e)))))

(defn new-app [router]
  (api-middleware
    (routes/with-routes
      (swagger-ui)
      (swagger-docs :title "Overlord")
      (exception-logging (api-routes router)))))

(defn ^Router new-router []
  (map->Router {}))