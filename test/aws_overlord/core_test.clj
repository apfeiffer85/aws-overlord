(ns aws-overlord.core-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :refer :all]
            [aws-overlord.core :refer :all]))

(defn- a-system [system]
  (is (every? (partial contains? system) [:http-server :router :storage])))

(deftest test-core-default
  (with-redefs [start identity]
    (is (a-system (-main)))))
