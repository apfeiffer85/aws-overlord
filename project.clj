(defproject aws-overlord "0.1.0-SNAPSHOT"
  :description "An AWS account coordinator."

  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [metosin/compojure-api "0.16.4"]
                 [metosin/ring-http-response "0.5.2"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 [org.clojure/tools.logging "0.2.4"]
                 [amazonica "0.2.30" :exclusions [joda-time]]]

  :main aws-overlord.core
  :uberjar-name "aws-overlord.jar"
  :profiles {:uberjar {:resource-paths ["swagger-ui"]}

             :free {:dependencies [[com.datomic/datomic-free "0.9.5067"]]}
             :pro {:dependencies [[com.datomic/datomic-pro "0.9.5067"]]
                   :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                                    :creds :gpg}}}

             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [org.slf4j/slf4j-simple "1.7.7"]]}})
