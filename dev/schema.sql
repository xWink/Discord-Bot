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
