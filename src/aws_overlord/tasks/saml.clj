(ns aws-overlord.tasks.saml
  (:require [clojure.data.json :as json]
            [amazonica.aws.identitymanagement :as iam]))

(defn- read-saml-document []
  (slurp "https://idp.zalando.net/shibboleth"))

(defn- create-saml-provider [document]
  (iam/create-samlprovider :name "Shibboleth" :saml-metadata-document document))

(defn- trust-policy-document [account-id]
  {"Version" "2012-10-17",
   "Statement" [{"Sid" "",
                 "Effect" "Allow",
                 "Principal" {"Federated" (str "arn:aws:iam::" account-id ":saml-provider/Shibboleth")},
                 "Action" "sts:AssumeRoleWithSAML",
                 "Condition" {"StringEquals" {"SAML:aud" "https://signin.aws.amazon.com/saml"}}}]
   })

(defn- create-role [policy-document]
  (iam/create-role :assume-role-policy-document (json/write-str policy-document)
                   :path "/"
                   :role-name "Shibboleth-Admin"))

; TODO rollback?
(defn run [{:keys [account/id] :as account}]
  (create-saml-provider (read-saml-document))
  (create-role (trust-policy-document id)))