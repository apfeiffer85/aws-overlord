aws-overlord
============

Use [[Leinignen|http://leiningen.org/]]:

    $ lein run

or with a REPL:

    $ lein with-profile free repl
    > (require ['com.stuartsierra.component :as 'component])
    > (def system (component/start (new-system (new-config nil))))
