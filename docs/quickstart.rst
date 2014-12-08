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

http://localhost:8080

.. code-block:: json

    {
      "key-id": "",
      "access-key": "",
      "networks": [
        {
          "vpn-gateway-ip": "",
          "vpn-routes": [
            ""
          ],
          "region": "",
          "name-servers": [
            ""
          ],
          "cidr-block": ""
        }
      ]
    }


.. code-block:: bash

    $ curl --header "Content-Type: application/json" --data @account.json http://localhost:8080/accounts/foo