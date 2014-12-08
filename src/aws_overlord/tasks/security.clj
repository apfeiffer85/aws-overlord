(ns aws-overlord.tasks.security
  (:import (com.amazonaws.services.identitymanagement.model NoSuchEntityException))
  (:require [clojure.data.json :as json]
            [amazonica.aws.identitymanagement :as iam]
            [clojure.tools.logging :as log]))

(defn- saml-provider-exists? [account-id name]
  (let [arn (str "arn:aws:iam::" account-id ":saml-provider/" name)]
    (try
      (boolean (iam/get-samlprovider :saml-provider-arn arn))
      (catch NoSuchEntityException e
        false))))

(defn- create-saml-provider [account-id]
  (if-not (saml-provider-exists? account-id "Shibboleth")
    (do
      (log/info "Creating SAML provider")
      (iam/create-samlprovider :name "Shibboleth"
                               :saml-metadata-document (slurp "https://idp.zalando.net/shibboleth")))
    (log/info "SAML provider already exists")))

(def ^:private admin-policy
  {"Version" "2012-10-17"
   "Statement" [{"Action" "*"
                 "Resource" "*"
                 "Effect" "Allow"}]
   })

(def ^:private power-user-policy
  {
   "Version" "2012-10-17"
   "Statement" [{"Effect" "Allow"
                 "Action" [
                           "iam:AddRoleToInstanceProfile"
                           "iam:CreateInstanceProfile"
                           "iam:CreateRole"
                           "iam:DeleteInstanceProfile"
                           "iam:DeleteRole"
                           "iam:GetInstanceProfile"
                           "iam:ListRolePolicies"
                           "iam:ListRoles"
                           "iam:ListServerCertificates"
                           "iam:PassRole"
                           ; FIXME: PutRolePolicy allows privilege escalation!
                           "iam:PutRolePolicy"
                           "iam:RemoveRoleFromInstanceProfile"
                           ]
                 "Resource" "*"}
                {"Effect" "Allow"
                 "Action" "ec2:*"
                 "Condition" {"ForAnyValue:StringLike" {"ec2:Region" ["eu-central-1"
                                                                      "eu-west-1"]}}
                 "Resource" "*"}
                {"Effect" "Allow"
                 "NotAction" ["ec2:*"
                              "iam:*"]
                 "Resource" "*"}
                {"Effect" "Deny"
                 "Action" ["ec2:DeleteNetworkAcl"
                           "ec2:DeleteRoute"
                           "ec2:DeleteRouteTable"
                           "ec2:DeleteSubnet"
                           "ec2:DeleteVpc"
                           "ec2:DeleteVpcPeeringConnection"
                           "ec2:DeleteVpnConnection"
                           "ec2:DeleteVpnConnectionRoute"
                           "ec2:DeleteVpnGateway"]
                 "Resource" "*"}]
   })

(def ^:private read-only-policy
  {"Version" "2012-10-17"
   "Statement" [{"Action" ["appstream:Get*"
                           "autoscaling:Describe*"
                           "cloudformation:DescribeStacks"
                           "cloudformation:DescribeStackEvents"
                           "cloudformation:DescribeStackResource"
                           "cloudformation:DescribeStackResources"
                           "cloudformation:GetTemplate"
                           "cloudformation:List*"
                           "cloudfront:Get*"
                           "cloudfront:List*"
                           "cloudtrail:DescribeTrails"
                           "cloudtrail:GetTrailStatus"
                           "cloudwatch:Describe*"
                           "cloudwatch:Get*"
                           "cloudwatch:List*"
                           "directconnect:Describe*"
                           "dynamodb:GetItem"
                           "dynamodb:BatchGetItem"
                           "dynamodb:Query"
                           "dynamodb:Scan"
                           "dynamodb:DescribeTable"
                           "dynamodb:ListTables"
                           "ec2:Describe*"
                           "elasticache:Describe*"
                           "elasticbeanstalk:Check*"
                           "elasticbeanstalk:Describe*"
                           "elasticbeanstalk:List*"
                           "elasticbeanstalk:RequestEnvironmentInfo"
                           "elasticbeanstalk:RetrieveEnvironmentInfo"
                           "elasticloadbalancing:Describe*"
                           "elastictranscoder:Read*"
                           "elastictranscoder:List*"
                           "iam:List*"
                           "iam:Get*"
                           "kinesis:Describe*"
                           "kinesis:Get*"
                           "kinesis:List*"
                           "opsworks:Describe*"
                           "opsworks:Get*"
                           "route53:Get*"
                           "route53:List*"
                           "redshift:Describe*"
                           "redshift:ViewQueriesInConsole"
                           "rds:Describe*"
                           "rds:ListTagsForResource"
                           "s3:Get*"
                           "s3:List*"
                           "sdb:GetAttributes"
                           "sdb:List*"
                           "sdb:Select*"
                           "ses:Get*"
                           "ses:List*"
                           "sns:Get*"
                           "sns:List*"
                           "sqs:GetQueueAttributes"
                           "sqs:ListQueues"
                           "sqs:ReceiveMessage"
                           "storagegateway:List*"
                           "storagegateway:Describe*"
                           "trustedadvisor:Describe*"]
                 "Resource" "*"
                 "Effect" "Allow"}]
   })

(defn- role-exists? [name]
  (try
    (boolean (iam/get-role :role-name name))
    (catch NoSuchEntityException e
      false)))

(defn- role-policy-document [account-id]
  {"Version" "2012-10-17",
   "Statement" [{"Sid" "",
                 "Effect" "Allow",
                 "Principal" {"Federated" (str "arn:aws:iam::" account-id ":saml-provider/Shibboleth")},
                 "Action" "sts:AssumeRoleWithSAML",
                 "Condition" {"StringEquals" {"SAML:aud" "https://signin.aws.amazon.com/saml"}}}]
   })

(defn- create-role [role-name account-id]
  (if-not (role-exists? role-name)
    (do
      (log/info "Creating role" role-name)
      (iam/create-role :role-name role-name
                       :assume-role-policy-document (json/write-str (role-policy-document account-id))
                       :path "/"))
    (log/info "Role" role-name "already exists")))

(defn- role-policy-exists [name]
  (try
    (boolean (iam/get-role-policy :role-name name :policy-name name))
    (catch NoSuchEntityException e
      false)))

(defn- create-role-policy [name policy-document]
  (if-not (role-policy-exists name)
    (do
      (log/info "Creating role policy" name)
      (iam/put-role-policy :role-name name
                           :policy-name name
                           :policy-document (json/write-str policy-document)))
    (log/info "Role policy" name "already exists")))

(defn run [{:keys [aws-id]}]
  (log/info "Setting up security")
  (create-saml-provider aws-id)
  (create-role "Shibboleth-Administrator" aws-id)
  (create-role "Shibboleth-PowerUser" aws-id)
  (create-role "Shibboleth-ReadOnly" aws-id)
  (create-role-policy "Shibboleth-Administrator" admin-policy)
  (create-role-policy "Shibboleth-PowerUser" power-user-policy)
  (create-role-policy "Shibboleth-ReadOnly" read-only-policy))
