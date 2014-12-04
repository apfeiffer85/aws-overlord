CREATE TABLE IF NOT EXISTS account (
  id          SERIAL PRIMARY KEY,
  name        TEXT NOT NULL,
  aws_id      TEXT NOT NULL,
  key_id      TEXT NOT NULL,
  access_key  TEXT NOT NULL
);

CREATE UNIQUE INDEX ON account (name);
CREATE UNIQUE INDEX ON account (aws_id);
