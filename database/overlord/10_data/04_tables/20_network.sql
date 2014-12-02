CREATE TABLE IF NOT EXISTS network (
  id             SERIAL PRIMARY KEY,
  region         TEXT    NOT NULL,
  private_key    TEXT    NOT NULL,
  cidr_block     TEXT    NOT NULL,
  vpn_gateway_ip TEXT    NOT NULL,
  vpn_routes     TEXT [] NOT NULL,
  account_id     INT     NOT NULL,
  FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ON network (account_id, region);
