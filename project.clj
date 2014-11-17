(defproject aws-overlord "0.1.0-SNAPSHOT"
  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [metosin/compojure-api "0.16.4"]
                 [metosin/ring-http-response "0.5.2"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 [org.clojure/tools.logging "0.2.4"]]
  :main aws-overlord.core
  :uberjar-name "aws-overlord.jar"
  :profiles {:uberjar {:resource-paths ["swagger-ui"]}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [org.slf4j/slf4j-simple "1.7.7"]]}})
