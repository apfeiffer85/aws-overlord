(ns aws-overlord.tasks.stack
  (:import (com.amazonaws AmazonServiceException))
  (:require [clojure.string :refer [capitalize]]
            [clojure.data.json :as json]
            [amazonica.core :refer [with-credential]]
            [amazonica.aws.cloudformation :as cf]
            [aws-overlord.net :as net]
            [clojure.tools.logging :as log]))

(defn- cloud-trail []
  {"Type" "AWS::CloudTrail::Trail"
   "Properties" {"IncludeGlobalServiceEvents" true
                 "IsLogging" true
                 "S3BucketName" "zalando-aws-cloudtrail"
                 "S3KeyPrefix" "Zalando"}
   })

(defn- vpc [team-name cidr-block]
  {"Type" "AWS::EC2::VPC"
   "Properties" {"CidrBlock" cidr-block
                 "Tags" [{"Key" "Name"
                          "Value" team-name}]}
   })

(defn- internet-gateway []
  {"Type" "AWS::EC2::InternetGateway"
   "Properties" {"Tags" [{"Key" "Name"
                          "Value" "Internet Gateway"}]}
   })

(defn- internet-gateway-attachment [gateway-id]
  {"Type" "AWS::EC2::VPCGatewayAttachment"
   "Properties" {"VpcId" {"Ref" "Vpc"}
                 "InternetGatewayId" {"Ref" gateway-id}}
   })

(defn- route-table [name]
  {"Type" "AWS::EC2::RouteTable"
   "Properties" {
                 "VpcId" {"Ref" "Vpc"}
                 "Tags" [{"Key" "Name"
                          "Value" name}]}
   })

(defn- gateway-route [gateway route-table-id]
  {"Type" "AWS::EC2::Route"
   "Properties" {"DestinationCidrBlock" "0.0.0.0/0"
                 "GatewayId" {"Ref" gateway}
                 "RouteTableId" {"Ref" route-table-id}}
   })


(defn- instance-route [instance-id route-table-id]
  {"Type" "AWS::EC2::Route"
   "Properties" {"DestinationCidrBlock" "0.0.0.0/0"
                 "InstanceId" {"Ref" instance-id}
                 "RouteTableId" {"Ref" route-table-id}}
   })
(defn- subnet-route-table-association [route-table-id subnet-id]
  {"Type" "AWS::EC2::SubnetRouteTableAssociation"
   "Properties" {"RouteTableId" {"Ref" route-table-id}
                 "SubnetId" {"Ref" subnet-id}}
   })

(defn- subnet-internet-access-route-table-association [subnet-id]
  (subnet-route-table-association "InternetAccess" subnet-id))

(defn- nat-security-group [cidr-block]
  {"Type" "AWS::EC2::SecurityGroup"
   "Properties" {"GroupDescription" "Allow internet access through Nat instances"
                 "SecurityGroupIngress" [{"IpProtocol" -1
                                          "FromPort" -1
                                          "ToPort" -1
                                          "CidrIp" cidr-block}]
                 "VpcId" {"Ref" "Vpc"}
                 "Tags" [{"Key" "Name"
                          "Value" "NAT Security Group"}]}
   })

(defn- network-acl [name]
  {"Type" "AWS::EC2::NetworkAcl"
   "Properties" {"VpcId" {"Ref" "Vpc"}
                 "Tags" [{"Key" "Name"
                          "Value" name}]}
   })

(defn- network-acl-entry [network-acl-id cidr-block direction action & {:keys [priority]}]
  {"Type" "AWS::EC2::NetworkAclEntry"
   "Properties" {"CidrBlock" cidr-block
                 "Egress" (= :outbound direction)
                 "NetworkAclId" {"Ref" network-acl-id}
                 "Protocol" -1
                 "RuleAction" (name action)
                 "RuleNumber" priority}
   })

(defn- subnet-network-acl-association [network-acl-id subnet-id]
  {"Type" "AWS::EC2::SubnetNetworkAclAssociation"
   "Properties" {"SubnetId" {"Ref" subnet-id}
                 "NetworkAclId" {"Ref" network-acl-id}}
   })

(defn- mmap [f coll]
  (into {} (map f coll)))

(defn- zone-index [availability-zone]
  (+ 1 (.indexOf (mapv char (range 97 123)) (last availability-zone))))

(defn- type-name [type]
  (-> type name capitalize))

(defn- subnet-id [{:keys [type availability-zone]}]
  (str (type-name type) "SubnetAz" (zone-index availability-zone)))

(defn- subnet-name [{:keys [type]}]
  (str (type-name type) " Subnet"))

(defn- subnet-route-id [{:keys [type availability-zone]}]
  (str (type-name type) "SubnetRouteAz" (zone-index availability-zone)))

(defn- nat-id [{:keys [availability-zone]}]
  (str "NatAz" (zone-index availability-zone)))

(defn- new-subnet [{:keys [availability-zone cidr-block] :as subnet}]
  {"Type" "AWS::EC2::Subnet"
   "Properties" {"VpcId" {"Ref" "Vpc"}
                 "CidrBlock" cidr-block
                 "AvailabilityZone" availability-zone
                 "Tags" [{"Key" "Name"
                          "Value" (subnet-name subnet)}]}
   })

(defn- nat-instance [availability-zone subnet-id]
  {"Type" "AWS::EC2::Instance"
   "Properties" {"AvailabilityZone" availability-zone
                 "DisableApiTermination" true
                 "ImageId" "ami-30913f47"                   ; current version of amzn-ami-vpc-nat-pv AMI
                 "InstanceType" "m1.small"
                 "KeyName" "overlord"
                 "Monitoring" true
                 "SecurityGroupIds" [{"Ref" "NatSecurityGroup"}]
                 "SourceDestCheck" false
                 "SubnetId" {"Ref" subnet-id}
                 "Tenancy" "dedicated"
                 "Tags" [{"Key" "Name"
                          "Value" "NAT"}]}
   })

(defn- elastic-ip [instance-id]
  {"Type" "AWS::EC2::EIP"
   "Properties" {"InstanceId" {"Ref" instance-id}}
   })

(defn- deny-duplicate-keys [&]
  (throw (ex-info "Duplicate key" {})))

(defn- setup-public [availability-zone subnets]
  (let [zone-index (zone-index availability-zone)]
    (merge-with deny-duplicate-keys
                (mmap (juxt subnet-id new-subnet) subnets)
                (mmap (juxt subnet-route-id (comp subnet-internet-access-route-table-association subnet-id)) subnets)
                (mmap (juxt nat-id (comp (partial nat-instance availability-zone) subnet-id)) subnets)
                {(str "NatRouteTableAz" zone-index) (route-table (str "NAT " availability-zone))
                 (str "NatDefaultRouteAz" zone-index) (instance-route (str "NatAz" zone-index) (str "NatRouteTableAz" zone-index))
                 (str "NatEipAz" zone-index) (elastic-ip (str "NatAz" zone-index))
                 })))

(defn- setup-shared [availability-zone subnets]
  (merge-with deny-duplicate-keys
              (mmap (juxt subnet-id new-subnet) subnets)))

(defn- setup-private [availability-zone subnets]
  (merge-with deny-duplicate-keys
              (mmap (juxt subnet-id new-subnet) subnets)
              ; TODO per subnet, not only once!
              {(str "PrivateNatRouteTableAssociationAz" (zone-index availability-zone))
               (subnet-route-table-association (str "NatRouteTableAz" (zone-index availability-zone))
                                               (str "PrivateSubnetAz" (zone-index availability-zone)))}
              (mmap (juxt (constantly (str "PrivateNetworkAclAssociationAz" (zone-index availability-zone)))
                          (partial subnet-network-acl-association "PrivateNetworkAcl")) (map subnet-id subnets))))

(defn- availability-zone [availability-zone subnets]
  (let [{:keys [public shared private]} (group-by :type subnets)]
    (merge-with deny-duplicate-keys
                (setup-public availability-zone public)
                (setup-shared availability-zone shared)
                (setup-private availability-zone private))))

(defn- availability-zones [subnets]
  (let [groups (group-by :availability-zone subnets)]
    (apply merge-with deny-duplicate-keys
           (map (partial apply availability-zone) groups))))

(defn- resources [{:keys [name]} {:keys [cidr-block subnets]}]
  (merge-with deny-duplicate-keys
              {"CloudTrail" (cloud-trail)
               "Vpc" (vpc name cidr-block)
               "InternetGateway" (internet-gateway)
               "InternetGatewayAttachment" (internet-gateway-attachment "InternetGateway")
               "InternetAccess" (route-table "Internet Access")
               "InternetGatewayRoute" (gateway-route "InternetGateway" "InternetAccess")
               "NatSecurityGroup" (nat-security-group cidr-block)
               "PrivateNetworkAcl" (network-acl "Private Network")
               "AllowInboundPrivateNetworkAclEntry" (network-acl-entry "PrivateNetworkAcl" cidr-block :inbound :allow :priority 100)
               "AllowOutboundPrivateNetworkAclEntry" (network-acl-entry "PrivateNetworkAcl" cidr-block :outbound :allow :priority 100)}
              (availability-zones subnets)))

(defn generate-template [account network]
  {"AWSTemplateFormatVersion" "2010-09-09"
   "Description" "Overlord Account Setup"
   "Resources" (resources account network)})

(defn- stack-exists? [name]
  (try
    (not (empty? (cf/describe-stacks :stack-name name)))
    (catch AmazonServiceException e
      false)))

(defn create-stack [name account network]
  (if-not (stack-exists? name)
    (do
      (log/info "Creating stack" name)
      (cf/create-stack :stack-name name
                       :template-body (json/write-str (generate-template account network))))
    (log/info "Stack" name "already exists")))
