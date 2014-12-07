CREATE TABLE account (
  id         SERIAL PRIMARY KEY,
  name       TEXT NOT NULL,
  aws_id     TEXT NOT NULL,
  key_id     TEXT NOT NULL,
  access_key TEXT NOT NULL
);

CREATE UNIQUE INDEX ON account (name);
CREATE UNIQUE INDEX ON account (aws_id);

CREATE TABLE network (
  id             SERIAL PRIMARY KEY,
  region         TEXT    NOT NULL,
  private_key    TEXT    NOT NULL,
  cidr_block     TEXT    NOT NULL,
  vpn_gateway_ip TEXT    NOT NULL,
  vpn_routes     TEXT [] NOT NULL,
  name_servers   TEXT [] NOT NULL,
  account_id     INT     NOT NULL,
  FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ON network (account_id, region);

CREATE TABLE subnet (
  type              TEXT NOT NULL,
  availability_zone TEXT NOT NULL,
  cidr_block        TEXT NOT NULL,
  network_id        INT  NOT NULL,
  FOREIGN KEY (network_id) REFERENCES network (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ON subnet (network_id, cidr_block);
