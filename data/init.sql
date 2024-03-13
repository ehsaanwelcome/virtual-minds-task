CREATE TABLE IF NOT EXISTS customer(
  id BIGSERIAL,
  name VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ip_blacklist (
  ip BIGINT NOT NULL CHECK (ip >= 0),
  PRIMARY KEY (ip)
);

CREATE TABLE IF NOT EXISTS ua_blacklist (
  ua VARCHAR(255) NOT NULL,
  PRIMARY KEY (ua)
);

CREATE TABLE IF NOT EXISTS hourly_stats (
  id BIGSERIAL,
  customer_id BIGINT NOT NULL CHECK (customer_id > 0),
  time timestamp NOT NULL,
  request_count BIGINT NOT NULL DEFAULT 0 CHECK (request_count >= 0),
  invalid_count BIGINT NOT NULL DEFAULT 0 CHECK (invalid_count >= 0),
  PRIMARY KEY (id),
  UNIQUE (customer_id, time),
  CONSTRAINT hourly_stats_customer_id FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE ON UPDATE NO ACTION
);
CREATE INDEX customer_idx ON hourly_stats(customer_id);

INSERT INTO customer VALUES (1,'Big News Media Corp',true),(2,'Online Mega Store',true),(3,'Nachoroo Delivery',false),(4,'Euro Telecom Group',true);
INSERT INTO ip_blacklist VALUES (0),(2130706433),(4294967295);
INSERT INTO ua_blacklist VALUES ('A6-Indexer'),('Googlebot-News'),('Googlebot');