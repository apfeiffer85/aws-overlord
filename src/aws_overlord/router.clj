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
                   :access-key String})

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
                   (schema/optional-key :lead) Member})

(defrecord Router [])

(defn- api-routes [router]
  (routes/with-routes

    (swaggered
      "Applications"
      :description "Application management operations"

      (POST*
        "/applications/:name" []
        :summary "Creates an application"
        :path-params [name :- String]
        :body [app ApplicationCreation]
        (log/info "Creating application" name)
        {:status 202})

      (GET*
        "/applications/:name" []
        :summary "Retrieves an application"
        :path-params [name :- String]
        :return ApplicationView
        (log/info "Retrieving application" name)
        {:status 200
         :body {:name name}})

      (DELETE*
        "/applications/:name" []
        :summary "Deletes an application"
        :path-params [name :- String]
        (log/info "Deleting application" name)
        {:status 204}))

    (swaggered
      "Teams"
      :description "Team management operations"

      (POST*
        "/teams/:name" []
        :summary "Creates a team"
        :path-params [name :- String]
        :body [team TeamCreation]
        (log/info "Creating team" name)
        {:status 202})

      (GET*
        "/teams/:name" []
        :summary "Retrieves a team"
        :path-params [name :- String]
        :return TeamView
        (log/info "Retrieving team" name)
        {:status 200
         :body {:name name
                :lead {:id "alice"}
                :members [{:id "bob"}
                          {:id "charlie"}]}})

      (PATCH*
        "/teams/:name" []
        :summary "Updates a team"
        :path-params [name :- String]
        :body [team TeamUpdate]
        (log/info "Updating team" name)
        {:status 202})

      (DELETE*
        "/teams/:name" []
        :summary "Deletes a team"
        :path-params [name :- String]
        (log/info "Deleting team" name)
        {:status 202})

      (PUT*
        "/teams/:name/members/:member-id" []
        :summary "Updates a team"
        :path-params [name :- String, member-id :- String]
        :body [member Member]
        (log/info "Adding member" member-id "to team" name)
        {:status 202})

      (DELETE*
        "/teams/:name/members/:member-id" []
        :summary "Deletes a member from a team"
        :path-params [name :- String, member-id :- String]
        (log/info "Removing member" member-id "from team" name)
        {:status 202}))))

(defn new-app [router]
  (api-middleware
    (routes/with-routes
      (swagger-ui)
      (swagger-docs :title "Overlord")
      (api-routes router))))

(defn ^Router new-router []
  (map->Router {}))