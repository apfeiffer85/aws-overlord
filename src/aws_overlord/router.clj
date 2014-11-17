(ns aws-overlord.router
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :refer :all]
            [compojure.api.routes :as routes]
            [ring.util.http-response :refer :all]
            [schema.core :as schema]))

(schema/defschema ApplicationView
                  {:name String})

(schema/defschema ApplicationCreation
                  {})

(schema/defschema Account
                  {:key-id String
                   :key-secret String})

(schema/defschema Member
                  {:id String})

(schema/defschema TeamView
                  {:name String
                   :lead Member
                   :members [Member]})

(schema/defschema TeamCreation
                  {:account Account
                   :lead Member
                   (schema/optional-key :members) [Member]})

(schema/defschema TeamUpdate
                  {(schema/optional-key :account) {:key-secret String}
                   (schema/optional-key :lead) Member
                   (schema/optional-key :members) [Member]})

(defrecord Router [])

(defn- api-routes [api]
  (routes/with-routes

    (swaggered
      "Applications"
      :description "Application management operations"

      (POST*
        "/apps/:name" []
        :summary "Creates an application"
        :path-params [name :- String]
        :body [app ApplicationCreation]
        (log/info "Creating application" name)
        {})

      (GET*
        "/apps/:name" []
        :summary "Retrieves an application"
        :path-params [name :- String]
        :return ApplicationView
        (log/info "Retrieving application" name)
        {:body {:name name}})

      (DELETE*
        "/apps/:name" []
        :summary "Deletes an application"
        :path-params [name :- String]
        (log/info "Deleting application" name)
        {}))

    (swaggered
      "Teams"
      :description "Team management operations"

      (POST*
        "/teams/:name" []
        :summary "Creates a team"
        :path-params [name :- String]
        :body [team TeamCreation]
        (log/info "Creating team" name)
        {})

      (GET*
        "/teams/:name" []
        :summary "Retrieves a team"
        :path-params [name :- String]
        :return TeamView
        (log/info "Retrieving team" name)
        {:body {:name name
                :lead {:id "alice"}
                :members [{:id "bob"}
                          {:id "charlie"}]}})

      (PUT*
        "/teams/:name" []
        :summary "Updates a team"
        :path-params [name :- String]
        :body [team TeamUpdate]
        (log/info "Updating team" name)
        {}))))

(defn new-app [api]
  (api-middleware
    (routes/with-routes
      (swagger-ui)
      (swagger-docs :title "Overlord")
      (api-routes api))))

(defn ^Router new-router []
  (map->Router {}))