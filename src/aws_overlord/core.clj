(ns aws-overlord.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component :refer [using]]
            [environ.core :refer [env]]
            [aws-overlord.aws]
            [aws-overlord.api.http-server :refer [new-http-server]]
            [aws-overlord.api.router :refer [new-router]]
            [aws-overlord.data.storage :refer [new-storage]]
            [aws-overlord.config :as config]))

(defn- new-system [config]
  (let [{:keys [http db dns]} (config/parse config [:http :db :dns])]
    (component/system-map
      :http-server (using (new-http-server http) [:router])
      :router (using (new-router {:dns dns}) [:storage])
      :storage (new-storage db))))

(defn -main [& args]
  (let [system (new-system env)]

    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn [] (component/stop system))))

    (component/start system)))
