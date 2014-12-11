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

Set up your local database:

.. code-block:: bash

    $ psql -f resources/database/schema.sql overlord

Now either build the jar from scratch and run it with your local JRE:

.. code-block:: bash

    $ lein uberjar
    $ java -jar target/aws-overlord.jar

or you use the pre-packaged docker images:

.. code-block:: bash

    $ docker run -d -p 8080:8080 zalando/overlord:0.5

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

======================  ========  =============  ===============
Key                     Required  Default Value  Comment
======================  ========  =============  ===============
dns.account.key-id      no
dns.account.access-key  no
dns.domains             no                       Comma separated
http.port               no        8080
db.protocol             no        postgresql
db.host                 no        localhost
db.port                 no        5432
db.name                 no        overlord
db.user                 no        postgres
db.password             no        postgres
======================  ========  =============  ===============

Setting up an Account
=====================

Create ``account.json``:

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

.. code-block:: bash

    INFO Configuring account shop
    INFO Fetching all accounts
    INFO Current account set to shop
    INFO Current region set to eu-west-1
    INFO Creating key pair
    INFO Inserting into :account {:aws_id 428507217034, :name shop, :access_key <secret>, :key_id <secret>}
    INFO Inserting into :network {:cidr_block 10.2.0.0/19, :name_servers [10.2.137.5 10.2.138.5 10.2.139.5], :region eu-west-1}
    INFO Inserting into :subnet {:network_id 55, :type public, :availability_zone eu-west-1a, :cidr_block 10.2.0.0/24}
    INFO Inserting into :subnet {:network_id 55, :type private, :availability_zone eu-west-1a, :cidr_block 10.2.16.0/23}
    INFO Inserting into :subnet {:network_id 55, :type private, :availability_zone eu-west-1c, :cidr_block 10.2.20.0/23}
    INFO Inserting into :subnet {:network_id 55, :type shared, :availability_zone eu-west-1b, :cidr_block 10.2.9.0/24}
    INFO Inserting into :subnet {:network_id 55, :type private, :availability_zone eu-west-1b, :cidr_block 10.2.18.0/23}
    INFO Inserting into :subnet {:network_id 55, :type shared, :availability_zone eu-west-1c, :cidr_block 10.2.10.0/24}
    INFO Inserting into :subnet {:network_id 55, :type public, :availability_zone eu-west-1b, :cidr_block 10.2.1.0/24}
    INFO Inserting into :subnet {:network_id 55, :type public, :availability_zone eu-west-1c, :cidr_block 10.2.2.0/24}
    INFO Inserting into :subnet {:network_id 55, :type shared, :availability_zone eu-west-1a, :cidr_block 10.2.8.0/24}
    INFO Successfully inserted account
    INFO Fetching account shop
    INFO Configuring account shop
    INFO Current account set to shop
    INFO Performing account-wide actions
    INFO Setting up security
    INFO Creating SAML provider
    INFO Creating role Shibboleth-Administrator
    INFO Creating role Shibboleth-PowerUser
    INFO Creating role Shibboleth-ReadOnly
    INFO Creating role policy Shibboleth-Administrator
    INFO Creating role policy Shibboleth-PowerUser
    INFO Creating role policy Shibboleth-ReadOnly
    INFO Destructured :account into {:key-id <secret>, :access-key <secret>}
    INFO Destructured :domains into aws.zalando.,aws.zalando.net.
    INFO Setting up shop.aws.zalando.
    INFO Creating hosted zone shop.aws.zalando.
    INFO Current account set to aws
    INFO Adding resource records for shop.aws.zalando. to aws.zalando.
    INFO Setting up shop.aws.zalando.net.
    INFO Creating hosted zone shop.aws.zalando.net.
    INFO Current account set to aws
    INFO Adding resource records for shop.aws.zalando.net. to aws.zalando.net.
    INFO Current region set to eu-west-1
    INFO Performing region-wide actions in eu-west-1
    INFO Creating stack overlord
    INFO Waiting for stack creation, current status is CREATE_IN_PROGRESS
    INFO Stack creation finished
    INFO Current region set to eu-west-1
    INFO Peering shop and core
    INFO Current account set to core
    INFO Creating VPC peering connection between vpc-33f83040 and vpc-a53ac4c3
    INFO Current account set to core
    INFO Accepting VPC peering connection pcx-7c15cf15
    INFO Creating VPN routes in rtb-ee4c8f8b to 10.2.160.0/19
    INFO Creating VPN routes in rtb-eb4c8f8e to 10.2.160.0/19
    INFO Creating VPN routes in rtb-ec4c8f89 to 10.2.160.0/19
    INFO Creating VPN routes in rtb-907ab7f5 to 10.2.128.0/19
    INFO Creating VPN routes in rtb-9c7ab7f9 to 10.2.128.0/19
    INFO Creating VPN routes in rtb-9e7ab7fb to 10.2.128.0/19
    INFO Creating VPN routes in rtb-927ab7f7 to 10.2.128.0/19
    INFO Creating VPN routes in rtb-937ab7f6 to 10.2.128.0/19
    INFO Creating VPN routes in rtb-9d7ab7f8 to 10.2.128.0/19
