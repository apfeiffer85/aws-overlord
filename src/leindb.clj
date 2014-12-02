(ns leindb
 (:import (java.io File))
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           [clojure.java.jdbc :as jdbc]))

(def ^:private database
 (:database (edn/read-string (slurp (io/resource "config.edn")))))

(defn drop-tables []
 (println "Dropping tables")
 (jdbc/execute! database [(str "DROP TABLE IF EXISTS account CASCADE")])
 (jdbc/execute! database [(str "DROP TABLE IF EXISTS network CASCADE")])
 (jdbc/execute! database [(str "DROP TABLE IF EXISTS subnet CASCADE")]))

(def ^:private table-defs
 (filter #(.isFile %) (file-seq (io/file "database/overlord/10_data/04_tables"))))

(defn migrate []
 (doseq [table-def table-defs]
  (println "Executing" (.getPath table-def))
  (jdbc/execute! database [(slurp table-def)])))

(defn seed [])