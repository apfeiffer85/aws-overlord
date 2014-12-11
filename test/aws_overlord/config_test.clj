(ns aws-overlord.config-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [aws-overlord.config :as config]))

(def config
  {:http-port "8080"
   :db-port "5432"
   :db-username "user"
   :dns-account-key-id "key"
   :dns-account-access-key "secret"
   :dns-domains "aws.zalando.,aws.zalando.net."})

(deftest test-config
  (fact "Config parsing"
        (config/parse config [:http :db]) => {:http {:port "8080"}
                                                :db {:port "5432"
                                                     :username "user"}})

  (fact "Config parsing without matches"
        (config/parse config [:foo :bar]) => {:foo {}
                                                :bar {}})

  (fact "Nested config parsing"
        (config/parse config [:http :db :dns]) => {:http {:port "8080"}
                                                      :db {:port "5432"
                                                           :username "user"}
                                                      :dns {:account-key-id "key"
                                                            :account-access-key "secret"
                                                            :domains "aws.zalando.,aws.zalando.net."}})

  (fact "DNS parsing"
        (config/parse {:account-key-id "key"
                       :account-access-key "secret"
                       :domains "aws.zalando.,aws.zalando.net."}
                      [:account :domains]) => {:account {:key-id "key"
                                                           :access-key "secret"}
                                                 :domains "aws.zalando.,aws.zalando.net."})

  (fact "Config parsing without namespaces"
        (config/parse config []) => {}))
