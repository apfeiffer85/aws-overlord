(ns aws-overlord.data.schema)

(def account
  "CREATE TABLE IF NOT EXISTS account (
     id SERIAL PRIMARY KEY,
     name TEXT,
     aws_id TEXT NOT NULL,
     key_id TEXT NOT NULL,
     access_key TEXT NOT NULL,
     owner_email TEXT NOT NULL
   );")

(def network
  "CREATE TABLE IF NOT EXISTS network (
     id SERIAL PRIMARY KEY,
     region TEXT NOT NULL,
     private_key TEXT,
     cidr_block TEXT NOT NULL,
     account_id INT NOT NULL,
     FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE
   );")

(def subnet
  "CREATE TABLE IF NOT EXISTS subnet (
    type TEXT NOT NULL,
    availability_zone TEXT NOT NULL,
    cidr_block TEXT NOT NULL,
    network_id INT NOT NULL,
    FOREIGN KEY (network_id) REFERENCES network(id) ON DELETE CASCADE
  );")
