CREATE TABLE IF NOT EXISTS subnet (
  type              TEXT NOT NULL,
  availability_zone TEXT NOT NULL,
  cidr_block        TEXT NOT NULL,
  network_id        INT  NOT NULL,
  FOREIGN KEY (network_id) REFERENCES network (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ON subnet (network_id, cidr_block);
