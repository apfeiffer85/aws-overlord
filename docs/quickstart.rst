==========
Quickstart
==========

Prerequisites
=============

* An AWS account
* Key ID and Secret Access Key

Installation
============

.. code-block:: bash

    $ lein uberjar
    $ java -jar target/aws-overlord.jar

.. _configuration:

Configuration
=============

Configuration is done via system properties or environment variables.
Configuration entries can either be lower-cased and dot-separated system properties or

.. code-block:: bash

    $ java -D http.port=80 -jar aws-overlord.jar
    
upper-underscore environment variables:
    
.. code-block:: bash

    $ HTTP_PORT=80 java -jar aws-overlord.jar

The following keys are configurable:

===========  ========  =============
Key          Required  Default Value 
===========  ========  =============
http.port    no        8080 
db.protocol  no        postgresql
db.host      no        localhost 
db.port      no        5432 
db.name      no        overlord 
db.user      no        postgres 
db.password  no        postgres 
===========  ========  =============
