(ns aws-overlord.aws
  (:require [clojure.tools.logging :as log]
            [amazonica.core :refer [set-root-unwrapping!]]))

(set-root-unwrapping! true)

(def ^:dynamic ^:private *interceptors* nil)

(defn aws [api credentials & args]
  (if (nil? *interceptors*)
    (do
      (log/info "Calling amazon")
      (apply api credentials args))
    (let [interceptor (get *interceptors* api)]
      (when (not interceptor)
        (throw (ex-info (str "Missing interceptor") {:fn api :args args})))
      (apply interceptor credentials args))))

(defmacro with-interceptors [interceptors & body]
  `(binding [*interceptors* ~interceptors]
     ~@body))