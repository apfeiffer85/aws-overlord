(ns aws-overlord.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component :refer [using]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [aws-overlord.aws] ; needed for set-root-unwrapping!
            [aws-overlord.enforcer :refer [new-enforcer]]
            [aws-overlord.api.http-server :refer [new-http-server]]
            [aws-overlord.api.router :refer [new-router]]
            [aws-overlord.data.storage :refer [new-storage]]))

(defn- new-config [config-file]
  (edn/read-string (slurp (or config-file (io/resource "config.edn")))))

(defn- new-system [config]
  (let [{:keys [http-port database]} config]
    (component/system-map
      :http-server (using (new-http-server http-port) [:router])
      :router (using (new-router) [:storage :enforcer])
      :enforcer (using (new-enforcer) [:storage])
      :storage (new-storage database))))

(defn -main [& args]
  (let [config (new-config (first args))
        system (new-system config)]

    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn [] (component/stop system))))

    (component/start system)))
