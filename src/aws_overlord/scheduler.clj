(ns aws-overlord.scheduler
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at]))

(defrecord Scheduler [pool]
  component/Lifecycle

  (start [this]
    (if pool
      this
      (do
        (assoc this :pool (at/mk-pool)))))

  (stop [this]
    (at/stop-and-reset-pool! (:pool this) :stop)
    (dissoc this :pool)))

(defn ^Scheduler new-scheduler []
  (map->Scheduler {}))

(defn schedule-with [^Scheduler this & {:keys [function every]}]
  (at/every every function (:pool this)))
