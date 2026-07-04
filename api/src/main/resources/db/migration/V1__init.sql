CREATE TABLE users (
  id              BIGSERIAL PRIMARY KEY,
  username        VARCHAR(64)  NOT NULL UNIQUE,
  name            VARCHAR(64)  NOT NULL,
  password_hash   VARCHAR(100) NOT NULL,
  role            VARCHAR(16)  NOT NULL DEFAULT 'USER',
  employee_no     VARCHAR(32),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE institutions (
  id    BIGSERIAL PRIMARY KEY,
  key   VARCHAR(8)  NOT NULL UNIQUE,
  name  VARCHAR(32) NOT NULL,
  "order" INT      NOT NULL DEFAULT 0
);

CREATE TABLE islands (
  id              BIGSERIAL PRIMARY KEY,
  key             VARCHAR(16) NOT NULL UNIQUE,
  name            VARCHAR(32) NOT NULL,
  "order"         INT NOT NULL DEFAULT 0,
  institution_id  BIGINT NOT NULL REFERENCES institutions(id)
);

CREATE TABLE docs (
  id              BIGSERIAL PRIMARY KEY,
  title           VARCHAR(128) NOT NULL,
  category        VARCHAR(64),
  institution_id  BIGINT NOT NULL REFERENCES institutions(id),
  island_id       BIGINT NOT NULL REFERENCES islands(id),
  required        BOOLEAN NOT NULL DEFAULT FALSE,
  file_path       VARCHAR(256),
  file_type       VARCHAR(8),
  "order"         INT NOT NULL DEFAULT 0,
  active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE progress (
  user_id         BIGINT NOT NULL REFERENCES users(id),
  doc_id          BIGINT NOT NULL REFERENCES docs(id),
  status          VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
  progress_pct    INT NOT NULL DEFAULT 0,
  last_read_at    TIMESTAMPTZ,
  completed_at    TIMESTAMPTZ,
  PRIMARY KEY (user_id, doc_id)
);

CREATE TABLE island_states (
  user_id         BIGINT NOT NULL REFERENCES users(id),
  island_id       BIGINT NOT NULL REFERENCES islands(id),
  status          VARCHAR(16) NOT NULL DEFAULT 'LOCKED',
  PRIMARY KEY (user_id, island_id)
);
