(ns aws-overlord.tasks.security-test
  (:import (com.amazonaws.services.identitymanagement.model NoSuchEntityException))
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [aws-overlord.tasks.security :as security]
            [amazonica.aws.identitymanagement :as iam]))

(defn- not-found [entity]
  (NoSuchEntityException. (str entity " not found")))

(deftest test-security
  (facts "Security should be set up correctly"
         (fact "Missing security entites should get created"
               (security/run {:aws-id "123"}) => nil
               (provided
                 (iam/get-samlprovider :saml-provider-arn "arn:aws:iam::123:saml-provider/Shibboleth") =throws=> (not-found "SAML Provider")
                 (iam/create-samlprovider :name "Shibboleth" :saml-metadata-document anything) => {}
                 (iam/get-role :role-name anything) =throws=> (not-found "Role") :times 3
                 (iam/create-role :role-name anything :assume-role-policy-document anything :path "/") => nil :times 3
                 (iam/get-role-policy :role-name anything :policy-name anything) =throws=> (not-found "Role Policy") :times 3
                 (iam/put-role-policy :role-name anything :policy-name anything :policy-document anything) => nil :times 3))
         (fact "Existing security entites should not be touched"
               (security/run {:aws-id "123"}) => nil
               (provided
                 (iam/get-samlprovider :saml-provider-arn "arn:aws:iam::123:saml-provider/Shibboleth") => {}
                 (iam/get-role :role-name anything) => {} :times 3
                 (iam/get-role-policy :role-name anything :policy-name anything) => {} :times 3))))