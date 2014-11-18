(ns aws-overlord.scheduler
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
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

(defn logging-execution [fun]
  (fn []
      (try
        (fun)
        (catch Exception e
          (log/error "Uncatched exception during job execution!" e)))))

(defn schedule-with [^Scheduler this & {:keys [function every]}]
  (at/every every (logging-execution function) (:pool this)))
