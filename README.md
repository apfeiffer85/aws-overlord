# Overlord

Overlord is an AWS account management tool. It supports one-time configuration, auditing and serves as a security gateway.

Accounting configuration is done in two phases:

### Manual Phase

- sign up for a new Amazon account
- set up consolidated billing
- remove billing information, e.g. credit card

### Automatic Phase

- Account Alias?
- Account ID = Account Alias?
- Account ID (parse from [GetUser](http://docs.aws.amazon.com/IAM/latest/APIReference/API_GetUser.html)?)

- [CreateKeyPair](http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateKeyPair.html)
- [CreateSAMLProvider](http://docs.aws.amazon.com/IAM/latest/APIReference/API_CreateSAMLProvider.html)
- [CreateRole](http://docs.aws.amazon.com/IAM/latest/APIReference/API_CreateRole.html) (SAML)
- set up AWS::Route53::HostedZone (team.aws.zalando)
- update internal name servers (TODO find out how)
- generate wildcard certificate \*.team.aws.zalando
- [UploadServerCertificate](http://docs.aws.amazon.com/IAM/latest/APIReference/API_UploadServerCertificate.html)
- per region (AWS::Region)
    - set up AWS::CloudTrail::Trail
    - set up AWS::EC2::VPC
    - per other team accounts
        - [CreateVpcPeeringConnection](http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateVpcPeeringConnection.html)
        - in the other team's account: [AcceptVpcPeeringConnection](http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-AcceptVpcPeeringConnection.html)
    - set up AWS::EC2::InternetGateway
    - set up AWS::EC2::RouteTable (Internet Access)
    - set up AWS::EC2::Route (Any → Internet Gateway)
    - per [availability zone](http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeAvailabilityZones.html)
        - set up AWS::EC2::Subnet (Public)
        - set up AWS::EC2::SubnetRouteTableAssociation (Public → Internet Access)
        - set up AWS::EC2::SecurityGroup (NAT)
        - set up AWS::EC2::Instance (NAT, using the latest `amzn-ami-vpc-nat-pv` AMI) (TODO [high availbilty setup](https://aws.amazon.com/articles/2781451301784570))
        - set up AWS::EC2::EIP (NAT)
        - set up AWS::EC2::RouteTable (NAT)
        - set up AWS::EC2::Route (Any → NAT)
        - set up AWS::EC2::Subnet (Shared)
        - set up AWS::EC2::NetworkAcl (Shared)
        - set up AWS::EC2::NetworkAclEntry (deny Public → Shared)
        - set up AWS::EC2::NetworkAclEntry (allow VPC → Shared)
        - set up AWS::EC2::SubnetNetworkAclAssociation (Shared → Shared)
        - set up AWS::EC2::Subnet (Private)
        - set up AWS::EC2::SubnetRouteTableAssociation (Private → NAT)

## API

![API](docs/api.png)

### Data

#### Account

- team name
- account id
- key id
- secret key
- key pair (generated)
- server certificate (generated)
- networks
    - region
    - cidr block
    - subnets (generated)
        - availability zone
        - cidr block
        - type (shared, private, public)

#### Access

- user name (LDAP)
- instance-id
- timestamp?!
- comment? (Ticket ID)

## Network Setup

Any given VPC is split up into three sections: *public* for internet-level load balancers,
*shared* for company-internal load balancers and *private* for team-internal instances.

Based on the bits of the VPC CIDR block:
- two bits are reserved for public, shared and private
    - 1/4 public subnets
    - 1/4 shared subnets
    - 2/4 private subnets
- three bits are reserved for a maximum of 8 availability zones
- only public and shared subnets of the same team are allowed to access private subnets
- only private subnets of the same team and the internet are allowed to access the public subnet
- TODO how can we restrict the communication between public and shared across the company?

## Development

Use [Leinignen](http://leiningen.org/):

    $ lein run

or run the test suite

    $ lein test

or with a REPL:

    $ lein repl
    > (require '[com.stuartsierra.component :as component])
    > (def system (component/start (new-system (new-config nil))))
