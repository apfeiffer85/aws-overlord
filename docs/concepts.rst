==============
Basic Concepts
==============

Overlord provides a RESTful API for accounts that expects the following format:

.. code-block:: json

    {
      "key-id": "AKIAIOSFODNN7EXAMPLE",
      "access-key": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
      "networks": [
        {
          "region": "eu-west-1",
          "cidr-block": "10.144.0.0./19",
          "vpn-gateway-ip": "93.184.216.119",
          "vpn-routes": [
            "10.10.0.0/16",
            "10.64.0.0/16"
          ],
          "name-servers": [
            "10.144.137.5",
            "10.144.138.5",
            "10.144.139.5"
          ]
        }
      ]
    }

``POST`` ing this to ``/accounts/foo`` will set up the Account *foo* by performing the following steps:

    - Create Key Pair *overlord*

    - Set up a SAML Provider

    - Create IAM roles and polices for

      - Administrator
      - PowerUser
      - ReadOnly

    - Create a Hosted Zone in Route 53

    - Perform the network setup as described :doc:`here <network-setup>`.



