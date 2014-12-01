(ns aws-overlord.logging
  (:import (org.slf4j MDC)))

(defmacro with-mdc [context & body]
  `(do
     (doseq [pair# ~context] (apply #(MDC/put %1 %2) pair#))
     ~@body
     (doseq [key# (keys ~context)] (apply #(MDC/remove %) key#))))