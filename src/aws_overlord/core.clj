(ns aws-overlord.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [aws-overlord.http-server :as http-server]
            [aws-overlord.router :as router]))

(defn new-system []
  (component/system-map
    :http-server (component/using
                   (http-server/new-http-server 8080)
                   [:router])
    :router (router/new-router)))

(defn -main [& args]
  (let [system (new-system)]
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn [] (component/stop system))))

    (component/start system)))
