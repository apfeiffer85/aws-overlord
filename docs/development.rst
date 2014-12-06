===========
Development
===========

Prerequisites
=============
- Java
- `Leiningen <http://leiningen.org/>`_
- PostgreSQL

Setup
=====

Tests are run against a system integrated with a local database. You'll need to set it
up manually:

.. code-block:: bash

    $ cat resources/database/schema.sql | psql overlord

Running
=======

You can run the main directly

.. code-block:: bash

    $ lein with-profile log run

or with a REPL:

.. code-block:: bash

    $ lein repl
    > (require '[com.stuartsierra.component :refer :all])
    > (def system (start (new-system {})))

Testing
=======

The following command will run the test suite:

.. code-block:: bash

    $ lein test
    
The same :ref:`way to configure <configuration>` also applies to the test suite:

.. code-block:: bash

    $ DB_USER=overlord lein test