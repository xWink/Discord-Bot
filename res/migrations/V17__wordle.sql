CREATE TABLE IF NOT EXISTS wordle_games (
    user    BIGINT      NOT NULL,
    day     BIGINT      NOT NULL,
    guesses VARCHAR(64) NOT NULL DEFAULT '',
    solved  TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (user, day)
);
