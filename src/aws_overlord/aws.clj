(ns aws-overlord.aws
  (:require [clojure.tools.logging :as log]
            [amazonica.core :refer [set-root-unwrapping!]]))

(set-root-unwrapping! true)
