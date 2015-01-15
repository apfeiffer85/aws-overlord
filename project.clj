(defproject aws-overlord "0.6"
  :description "An AWS account coordinator."

  :url "https://github.com/zalando/aws-overlord"

  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/"}

  :scm {:url "git@github.com:zalando/aws-overlord.git"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; lifecycle management
                 [com.stuartsierra/component "0.2.2"]
                 [environ "1.0.0"]
                 ; REST APIs
                 [metosin/compojure-api "0.16.4"]
                 [metosin/ring-http-response "0.5.2"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 ; logging
                 [org.clojure/tools.logging "0.2.4"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.slf4j/jul-to-slf4j "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 ; amazon aws (if upgrading, also check the joda-time version)
                 [amazonica "0.3.12" :exclusions [joda-time commons-logging]]
                 [joda-time "2.5"]
                 ; storage
                 [postgresql "9.3-1102.jdbc41"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [java-jdbc/dsl "0.1.1"]]

  :plugins [[lein-cloverage "1.0.2"]
            [lein-set-version "0.4.1"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :aliases {"cloverage" ["with-profile" "test" "cloverage"]}

  :main aws-overlord.core
  :aot :all
  :uberjar-name "aws-overlord.jar"
  :profiles {
             :log {:dependencies [[org.apache.logging.log4j/log4j-core "2.1"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]]}

             :no-log {:dependencies [[org.slf4j/slf4j-nop "1.7.7"]]}

             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}

             :test [:log {:dependencies [[org.clojars.runa/conjure "2.1.3"]
                                         [midje "1.6.3"]
                                         [clj-http "1.0.1"]]}]

             :repl [:no-log]

             :uberjar [:log {:resource-paths ["swagger-ui"]}]})
