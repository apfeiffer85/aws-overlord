=============
Network Setup
=============

AWS Overlord expects a preconfigured AWS VPC with the following network setup:

.. image:: _static/network-setup.png

Public Subnets
==============

Public subnets are reserved for public ELB load balancers and contain special NAT instances
allowing the shared and private subnets to reach the outside world.

Shared Subnets
==============

Shared subnets are reserved for company-wide ELB load balancers.
They are accessible from other peered VPCs (i.e. including other peered AWS accounts).

Private Subnets
===============

Private subnets contain all application instances (EC2 instances).
The private subnets are only accessible from within the containing VPC.
