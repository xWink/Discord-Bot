CREATE TABLE IF NOT EXISTS wager_settings (
    user      BIGINT  PRIMARY KEY,
    paid_play TINYINT NOT NULL DEFAULT 1
);
