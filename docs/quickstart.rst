==========
Quickstart
==========

Prerequisites
=============

* An AWS account
* Key ID and Secret Access Key
* PostgreSQL database

Installation
============

.. code-block:: bash

    $ psql -f resources/database/schema.sql overlord

.. code-block:: bash

    $ lein uberjar
    $ java -jar target/aws-overlord.jar

.. code-block:: bash

    $ docker run -d -p 8080:8080 zalando/overlord

.. _configuration:

Configuration
=============

Configuration is done via system properties or environment variables.
Configuration entries can either be lower-cased and dot-separated system properties

.. code-block:: bash

    $ java -D http.port=80 -jar aws-overlord.jar
    
or upper-underscore environment variables:
    
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

Setting up an Account
=====================

Create ``ccount.json``:

.. code-block:: json

    {
      "key-id": "AKIAIOSFODNN7EXAMPLE",
      "access-key": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
      "networks": [
        {
          "region": "eu-west-1",
          "cidr-block": "10.2.0.0./19",
          "vpn-gateway-ip": "93.184.216.119",
          "vpn-routes": [
            "10.10.0.0/16",
            "10.64.0.0/16"
          ],
          "name-servers": [
            "10.2.137.5",
            "10.2.138.5",
            "10.2.139.5"
          ]
        }
      ]
    }

``POST`` the file to Overlord:

.. code-block:: bash

    $ curl --header "Content-Type: application/json" --data @account.json http://localhost:8080/accounts/foo

.. code-block:: log

    ...