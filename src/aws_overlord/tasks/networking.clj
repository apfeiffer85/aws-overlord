(ns aws-overlord.tasks.networking
  (:import (com.amazonaws AmazonServiceException))
  (:require [clojure.string :refer [capitalize]]
            [clojure.data.json :as json]
            [amazonica.aws.cloudformation :as cf]
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
                 "EnableDnsSupport" true
                 "EnableDnsHostnames" false
                 "Tags" [{"Key" "Name"
                          "Value" team-name}]}
   })

(defn- dhcp-options [name-servers]
  {"Type" "AWS::EC2::DHCPOptions"
   "Properties" {"DomainNameServers" name-servers
                 "Tags" [{"Key" "Name"
                          "Value" "DNS"}]}
   })

(defn- dhcp-options-association [vpc-id dhcp-options-id]
  {"Type" "AWS::EC2::VPCDHCPOptionsAssociation"
   "Properties" {"DhcpOptionsId" {"Ref" dhcp-options-id}
                 "VpcId" {"Ref" vpc-id}}
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

(defn- vpn-gateway-attachment [vpn-gateway-id]
  {"Type" "AWS::EC2::VPCGatewayAttachment"
   "Properties" {"VpcId" {"Ref" "Vpc"}
                 "VpnGatewayId" {"Ref" vpn-gateway-id}}
   })

(defn- route-table [name & {:keys [vpc-routing] :or {vpc-routing false}}]
  {"Type" "AWS::EC2::RouteTable"
   "Properties" {
                 "VpcId" {"Ref" "Vpc"}
                 "Tags" [{"Key" "Name"
                          "Value" name}
                         {"Key" "VPC-Routing"
                          "Value" (str vpc-routing)}]}
   })

(defn- gateway-route [cidr-block gateway route-table-id]
  {"Type" "AWS::EC2::Route"
   "Properties" {"DestinationCidrBlock" cidr-block
                 "GatewayId" {"Ref" gateway}
                 "RouteTableId" {"Ref" route-table-id}}
   })

(defn- internet-gateway-route [route-table-id]
  (gateway-route "0.0.0.0/0" "InternetGateway" route-table-id))

(defn- instance-route [route-table-id instance-id]
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

(defn- subnet-public-route-table-association [subnet-id]
  (subnet-route-table-association "PublicRouteTable" subnet-id))

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

(defn- vpn-gateway [name]
  {"Type" "AWS::EC2::VPNGateway"
   "Properties" {"Type" "ipsec.1"
                 "Tags" [{"Key" "Name"
                          "Value" name}]}
   })

(defn- customer-gateway [name ip-address]
  {"Type" "AWS::EC2::CustomerGateway"
   "Properties" {"BgpAsn" 65000
                 "IpAddress" ip-address
                 "Tags" [{"Key" "Name"
                          "Value" name}]
                 "Type" "ipsec.1"}
   })

(defn- vpn-connection [name vpn-gateway-id customer-gateway-id]
  {"Type" "AWS::EC2::VPNConnection"
   "Properties" {"Type" "ipsec.1"
                 "CustomerGatewayId" {"Ref" customer-gateway-id}
                 "StaticRoutesOnly" true
                 "Tags" [{"Key" "Name"
                          "Value" name}]
                 "VpnGatewayId" {"Ref" vpn-gateway-id}}
   })

(defn- vpn-connection-route [vpn-connection-id cidr-block]
  {"Type" "AWS::EC2::VPNConnectionRoute"
   "Properties" {"DestinationCidrBlock" cidr-block
                 "VpnConnectionId" {"Ref" vpn-connection-id}}
   })

(defn- vpn-gateway-route-propagation [route-table-id]
  {"Type" "AWS::EC2::VPNGatewayRoutePropagation"
   "Properties" {"RouteTableIds" [{"Ref" route-table-id}]
                 "VpnGatewayId" {"Ref" "VpnGateway"}}
   "DependsOn" "VpnGatewayAttachment"
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

(defn- subnet-prefix-route-id [prefix {:keys [type availability-zone]}]
  (str (type-name type) prefix "SubnetRouteAz" (zone-index availability-zone)))

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
                 "DisableApiTermination" false
                 "ImageId" "ami-30913f47"                   ; current version of amzn-ami-vpc-nat-pv AMI
                 "InstanceType" "m3.medium"                 ; TODO make configurable
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

(defn- deny-duplicate-keys [& args]
  (throw (ex-info (str "Duplicate key for values: " args) {})))

(defn- setup-public [availability-zone subnets]
  (let [zone-index (zone-index availability-zone)]
    (merge-with deny-duplicate-keys
                (mmap (juxt subnet-id new-subnet) subnets)
                (mmap (juxt (partial subnet-prefix-route-id "Public")
                            (comp subnet-public-route-table-association subnet-id)) subnets)
                (mmap (juxt nat-id
                            (comp (partial nat-instance availability-zone) subnet-id)) subnets)
                {(str "NatEipAz" zone-index) (elastic-ip (str "NatAz" zone-index))})))

(defn- setup-shared [availability-zone subnets]
  (let [shared-route-table (str "SharedRouteTableAz" (zone-index availability-zone))]
    (merge-with deny-duplicate-keys
                (mmap (juxt subnet-id new-subnet) subnets)
                {shared-route-table (route-table (str "Shared " availability-zone) :vpc-routing true)
                 (str "SharedVpnGatewayRoutePropagationAz" (zone-index availability-zone)) (vpn-gateway-route-propagation shared-route-table)}
                (mmap (juxt (partial subnet-prefix-route-id "NatDefault")
                            (comp (partial instance-route shared-route-table) (partial str "NatAz") zone-index :availability-zone)) subnets)
                (mmap (juxt (constantly (str "SharedRouteTableAssociationAz" (zone-index availability-zone)))
                            (comp (partial subnet-route-table-association shared-route-table) subnet-id)) subnets))))

(defn- setup-private [availability-zone subnets]
  (let [private-route-table (str "PrivateRouteTableAz" (zone-index availability-zone))]
    (merge-with deny-duplicate-keys
                (mmap (juxt subnet-id new-subnet) subnets)
                {private-route-table (route-table (str "Private " availability-zone) :vpc-routing true)
                 (str "PrivateVpnGatewayRoutePropagationAz" (zone-index availability-zone)) (vpn-gateway-route-propagation private-route-table)}
                (mmap (juxt (partial subnet-prefix-route-id "NatDefault")
                            (comp (partial instance-route private-route-table) (partial str "NatAz") zone-index :availability-zone)) subnets)
                (mmap (juxt (constantly (str "PrivateRouteTableAssociationAz" (zone-index availability-zone)))
                            (comp (partial subnet-route-table-association private-route-table) subnet-id)) subnets))))

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

(defn- dhcp [name-servers]
  (if (empty? name-servers)
    {}
    {"DhcpOptions" (dhcp-options name-servers)
     "DhcpOptionsAssociation" (dhcp-options-association "Vpc" "DhcpOptions")}))

(defn- vpn-connection-routes [vpn-routes]
  (into {} (map-indexed (fn [index cidr-block]
                          [(str "VpnConnectionRoute" (inc index))
                           (vpn-connection-route "VpnConnection" cidr-block)]) vpn-routes)))

(defn- resources [{:keys [name]} {:keys [cidr-block vpn-gateway-ip vpn-routes name-servers subnets]}]
  (merge-with deny-duplicate-keys
              {"CloudTrail" (cloud-trail)
               "Vpc" (vpc name cidr-block)
               "InternetGateway" (internet-gateway)
               "InternetGatewayAttachment" (internet-gateway-attachment "InternetGateway")
               "PublicRouteTable" (route-table "Public")
               "PublicInternetGatewayRoute" (internet-gateway-route "PublicRouteTable")
               "NatSecurityGroup" (nat-security-group cidr-block)
               "VpnGateway" (vpn-gateway "VPN Gateway")
               "VpnGatewayAttachment" (vpn-gateway-attachment "VpnGateway")
               "CustomerGateway" (customer-gateway "Customer Gateway" vpn-gateway-ip)
               "VpnConnection" (vpn-connection "VPN Connection" "VpnGateway" "CustomerGateway")}
              (dhcp name-servers)
              (vpn-connection-routes vpn-routes)
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

(defn- stack-status [name]
  (-> (cf/describe-stacks :stack-name name) first :stack-status))

(defn run [account network & {:keys [sleep-timeout]
                              :or {sleep-timeout 5000}}]
  (let [stack-name "overlord"]
    (if-not (stack-exists? stack-name)
      (do
        (log/info "Creating stack overlord")
        (cf/create-stack :stack-name stack-name
                         :template-body (json/write-str (generate-template account network)))
        (loop [v (repeat 1)]
          (let [status (stack-status stack-name)]
            (condp = status
              "CREATE_IN_PROGRESS" (do
                                     (log/info "Waiting for stack creation, current status is" status)
                                     (Thread/sleep sleep-timeout)
                                     (recur v))
              "CREATE_COMPLETE" (log/info "Stack creation finished")
              (throw (IllegalStateException. (str "Stack creation failed with status " status)))))))
      (log/info "Stack overlord already exists"))))
