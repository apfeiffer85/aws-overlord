===========
Development
===========

Installing
==========

Docker!

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

    $ psql -f resources/database/schema.sql overlord


Testing
=======

The following command will run the test suite:

.. code-block:: bash

    $ lein test
    
The same :ref:`way to configure <configuration>` also applies to the test suite:

.. code-block:: bash

    $ DB_USER=overlord lein test