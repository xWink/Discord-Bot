-- Minimal legacy tables the migrations extend. Inferred from the connectors;
-- use only for a local dev database, never against production.
CREATE TABLE IF NOT EXISTS economy (
    user        BIGINT PRIMARY KEY,
    wallet      INT NOT NULL DEFAULT 0,
    role_expiry BIGINT NOT NULL DEFAULT 0,
    role_colour VARCHAR(40) NULL
);

CREATE TABLE IF NOT EXISTS bang (
    user        BIGINT PRIMARY KEY,
    tries       INT NOT NULL DEFAULT 0,
    deaths      INT NOT NULL DEFAULT 0,
    jams        INT NOT NULL DEFAULT 0,
    last_played BIGINT NOT NULL DEFAULT 0,
    last_daily  BIGINT NOT NULL DEFAULT 0,
    streak      INT NOT NULL DEFAULT 0,
    death_rate  DOUBLE AS (IF(tries = 0, 0, deaths / tries)) VIRTUAL
);

CREATE TABLE IF NOT EXISTS karma (
    user      BIGINT PRIMARY KEY,
    upvotes   INT NOT NULL DEFAULT 0,
    downvotes INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ping (
    user BIGINT PRIMARY KEY,
    `max` INT NOT NULL DEFAULT 0,
    `min` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    message_id   BIGINT PRIMARY KEY,
    author_id    BIGINT NOT NULL,
    channel_id   BIGINT NOT NULL,
    time_sent    BIGINT NOT NULL,
    content      TEXT NULL,
    image_base64 MEDIUMTEXT NULL
);
CREATE TABLE IF NOT EXISTS messages_backup LIKE messages;
