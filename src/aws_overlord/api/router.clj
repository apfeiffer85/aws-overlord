(ns aws-overlord.api.router
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :refer :all]
            [compojure.api.routes :as routes]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [aws-overlord.data.storage :refer [insert-account delete-account account-by-name]]
            [aws-overlord.api.mapping :refer :all]))

(def ^:private opt s/optional-key)

(s/defschema AccountView
             {:name String
              :networks [{:region String
                          :cidr-block String
                          :subnets [{:availability-zone String
                                     :cidr-block String}]}]
              :owner-email String})

(s/defschema AccountCreation
             {:credentials {:key-id String
                            :access-key String}
              :networks [{:region String
                          :cidr-block String
                          :subnets [{:availability-zone String
                                     :cidr-block String}]}]
              :owner-email String})

(s/defschema AccountUpdate
             {:credentials {:access-key String}
              :networks [{:region String
                          :cidr-block String
                          :subnets [{:availability-zone String
                                     :cidr-block String}]}]
              :owner-email String})

(s/defschema AccountPatch
             {(opt :credentials) {(opt :access-key) String}
              (opt :networks) [{(opt :region) String
                                (opt :cidr-block) String
                                (opt :subnets) [{(opt :availability-zone) String
                                                 (opt :cidr-block) String}]}]
              (opt :owner-email) String})

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
        :body [account AccountCreation]
        (log/info "Configuring account" name)
        (let [{:keys [storage]} router
              named-account (assoc account :name name)]
          (insert-account storage (account-to-db named-account))
          {:status 202}))

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

      (PUT*
        "/accounts/:name" []
        :summary "Updates an account"
        :path-params [name :- String]
        :body [account AccountUpdate]
        (log/info "Updating account" name)
        {:status 202})

      (PATCH*
        "/accounts/:name" []
        :summary "Updates an account"
        :path-params [name :- String]
        :body [account AccountPatch]
        (log/info "Patching account" name)
        {:status 202})

      (DELETE*
        "/accounts/:name" []
        :summary "Deletes an account"
        :path-params [name :- String]
        (log/info "Deleting account" name)
        (let [{:keys [storage]} router]
          (delete-account storage name))
        {:status 202}))))

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