(ns aws-overlord.http-server
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :refer [api-middleware]]
            [aws-overlord.router :as router]))

(defn- exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Caught exception in web-tier")
        (throw e)))))

(defrecord HTTPServer [port router server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (let [server (jetty/run-jetty (exception-logging (router/new-app router)) {:port port :join? false})]
        (assoc this :server server))))

  (stop [this]
    (if server
      (.stop server)
      this)))

(defn ^HTTPServer new-http-server [port]
  (map->HTTPServer {:port port}))