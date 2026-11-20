CREATE TABLE IF NOT EXISTS profile_prefs (
    user     BIGINT  PRIMARY KEY,
    animated TINYINT NOT NULL DEFAULT 0
);
