(ns aws-overlord.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [aws-overlord.enforcer :as enforcer]
            [aws-overlord.http-server :as http-server]
            [aws-overlord.router :as router]
            [aws-overlord.storage :as storage]
            [aws-overlord.scheduler :as scheduler]))

(defn- new-config [config-file]
  (edn/read-string (slurp (or config-file (io/resource "config.edn")))))

(defn- new-system [config]
  (let [{:keys [http-port datomic-url]} config]
    (component/system-map

      :enforcer (component/using
                  (enforcer/new-enforcer)
                  [:scheduler :storage])

      :http-server (component/using
                     (http-server/new-http-server http-port)
                     [:router])

      :router (router/new-router)

      :storage (storage/new-storage datomic-url)

      :scheduler (scheduler/new-scheduler))))

(defn -main [& args]
  (let [config (new-config (first args))
        system (new-system config)]

    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn [] (component/stop system))))

    (component/start system)))
