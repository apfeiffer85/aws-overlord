(ns aws-overlord.config-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [aws-overlord.config :as config]))

(def config
  {:http-port "8080"
   :db-port "5435"
   :db-username "user"})

(deftest test-config
  (fact "Config parsing"
        (config/parse config ["http" "db"]) => {:http {:port "8080"}
                                                :db {:port "5435"
                                                     :username "user"}})
  
  (fact "Config parsing without matches"
        (config/parse config ["foo" "bar"]) => {:foo {}
                                                :bar {}})

  (fact "Config parsing without namespaces"
        (config/parse config []) => {}))
