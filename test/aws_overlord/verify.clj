(ns aws-overlord.verify
  (:require [clojure.test :refer :all]
            [clojure.string :refer [join]]
            [conjure.core :refer :all]))


(defmacro verify-nth-call-args-for-indices
  "Asserts that the function was called at least once, and the nth call was
   passed the args specified, into the indices of the arglist specified. In
   other words, it checks only the particular args you care about."
  [n fn-name indices & args]
  `(do
     (assert-in-fake-context "verify-first-call-args-for-indices")
     (assert-conjurified-fn "verify-first-call-args-for-indices" ~fn-name)
     (is (= true (pos? (count (get @call-times ~fn-name))))
         (str "(verify-first-call-args-for-indices " ~fn-name " " ~indices " " ~(join " " args) ")"))
     (let [first-call-args# (nth (get @call-times ~fn-name) ~n)
           indices-in-range?# (< (apply max ~indices) (count first-call-args#))]
       (if indices-in-range?#
         (is (= ~(vec args) (map #(nth first-call-args# %) ~indices))
             (str "(verify-first-call-args-for-indices " ~fn-name " " ~indices " " ~(join " " args) ")"))
         (is (= :fail (format "indices %s are out of range for the args, %s" ~indices ~(vec args)))
             (str "(verify-first-call-args-for-indices " ~fn-name " " ~indices " " ~(join " " args) ")"))))))