(defproject aws-overlord "0.1.0-SNAPSHOT"
  :description "An AWS account coordinator."

  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; lifecycle management
                 [com.stuartsierra/component "0.2.2"]
                 ; REST APIs
                 [metosin/compojure-api "0.16.4"]
                 [metosin/ring-http-response "0.5.2"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 ; logging
                 [org.clojure/tools.logging "0.2.4"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 ; amazon aws (if upgrading, also check the joda-time version)
                 [amazonica "0.2.30" :exclusions [joda-time commons-logging]]
                 [joda-time "2.5"]
                 ; scheduling
                 [overtone/at-at "1.2.0"]]

  :main aws-overlord.core
  :uberjar-name "aws-overlord.jar"
  :profiles {:uberjar {:resource-paths ["swagger-ui"]}

             :free {:dependencies [[com.datomic/datomic-free "0.9.5067"
                                    :exclusions [joda-time commons-logging org.slf4j/slf4j-nop org.slf4j/log4j-over-slf4j]]]}

             :pro {:dependencies [[com.datomic/datomic-pro "0.9.5067"
                                   :exclusions [joda-time commons-logging org.slf4j/slf4j-nop org.slf4j/log4j-over-slf4j]]]
                   :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                                    :creds :gpg}}}

             :prod {:dependencies [[org.slf4j/jul-to-slf4j "1.7.7"]
                                   [org.slf4j/jcl-over-slf4j "1.7.7"]
                                   [org.apache.logging.log4j/log4j-core "2.1"]
                                   [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]]}

             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}

             :test [:free {:dependencies [[org.slf4j/slf4j-simple "1.7.7"]]}]

             :repl [:free {:dependencies [[org.slf4j/slf4j-nop "1.7.7"]]}]})
