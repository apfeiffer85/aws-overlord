(ns aws-overlord.api.router
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :refer :all]
            [compojure.api.routes :as routes]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [aws-overlord.logging :refer [with-mdc]]
            [aws-overlord.data.storage :refer :all]
            [aws-overlord.logic.accounts :as accounts]
            [aws-overlord.net :refer :all]
            [aws-overlord.data.storage :as storage]))

(s/defschema AccountView
             {:name String
              :networks [{:region String
                          :cidr-block String
                          :vpn-gateway-ip String
                          :vpn-routes [String]
                          :name-servers [String]
                          :subnets [{:type String
                                     :availability-zone String
                                     :cidr-block String}]}]})

(s/defschema AccountCreation
             {:key-id String
              :access-key String
              :networks [{:region String
                          :cidr-block String
                          :vpn-gateway-ip String
                          :vpn-routes [String]
                          :name-servers [String]}]})

(defrecord Router [storage])

(defn- outbound-subnet [subnet]
  (-> subnet
      (dissoc :id :network-id)
      (update-in [:type] name)))

(defn- outbound-network [network]
  (-> network
      (dissoc :id :private-key :account-id)
      (update-in [:subnets] (partial mapv outbound-subnet))))

(defn- outbound [account]
  (-> account
      (dissoc :id :key-id :access-key :aws-id)
      (update-in [:networks] (partial mapv outbound-network))))

(defn- api-routes [{:keys [storage]}]
  (routes/with-routes

    (swaggered
      "System"
      :description "Basic system operations"

      (GET*
        "/health" []
        :summary "Performs a health check"
        (log/info "Checking health")
        ; TODO perform a database connection
        {:status 200}))

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
        (let [existing-accounts (storage/all-accounts storage)]
          (insert-account storage (accounts/prepare name account))
          (let [new-account (account-by-name storage name)]
            (accounts/configure new-account existing-accounts)
            {:status 202
             :body (outbound new-account)})))

      (GET*
        "/accounts" []
        :summary "Retrieves all accounts"
        :return [AccountView]
        {:status 200 :body (mapv outbound (all-accounts storage))})

      (GET*
        "/accounts/:name" []
        :summary "Retrieves an account"
        :path-params [name :- String]
        :return AccountView
        (let [account (account-by-name storage name)]
          (if account
            {:status 200 :body (outbound account)}
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