aws-overlord
============

Use [[Leinignen|http://leiningen.org/]]: 

    $ lein run

or with a REPL:

    $ lein repl
    > (require ['com.stuartsierra.component :as 'component]) 
    > (def system (component/start (new-system))
    > (pprint system)
