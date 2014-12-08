============
AWS Overlord
============

.. image:: https://travis-ci.org/zalando/aws-overlord.svg?branch=master
   :target: https://travis-ci.org/zalando/aws-overlord
   :alt: Build Status

.. image:: https://readthedocs.org/projects/aws-overlord/badge/?version=latest
   :target: https://aws-overlord.readthedocs.org
   :alt: Documentation Status

.. image:: https://coveralls.io/repos/zalando/aws-overlord/badge.png
   :target: https://coveralls.io/r/zalando/aws-overlord
   :alt: Coverage Status

Overlord is an AWS account management tool. It supports one-time configuration, auditing
and serves as a security gateway.

Requirements
============

- Java 1.7+
- `Leiningen <http://leiningen.org/>`_
- PostgreSQL 9.3+

Building
========

.. code-block:: bash

    $ lein uberjar
    
Running
=======

.. code-block:: bash

    $ psql -f resources/database/schema.sql overlord
    $ java -jar target/aws-overlord.jar

Running tests
=============

.. code-block:: bash

    $ lein test

Releasing
=========

.. code-block:: bash

    $ lein test
    $ VERSION=0.2
    $ echo $VERSION > version
    $ lein uberjar
    $ git tag $VERSION
    $ git push --tags
    $ docker build -t zalando/overlord:$VERSION .
    $ docker push zalando/overlord:$VERSION

Documentation
=============

See the `AWS Overlord Documentation on Read the Docs <http://aws-overlord.readthedocs.org>`_.

Building HTML documentation locally:

.. code-block:: bash

   $ cd docs
   $ make html

TODO
====

- AWS mocks for testing
- multi-region support