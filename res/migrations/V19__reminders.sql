CREATE TABLE IF NOT EXISTS reminders (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user    BIGINT       NOT NULL,
    channel BIGINT       NOT NULL,
    text    VARCHAR(400) NOT NULL,
    due     BIGINT       NOT NULL,
    here    TINYINT      NOT NULL DEFAULT 0,
    done    TINYINT      NOT NULL DEFAULT 0,
    created BIGINT       NOT NULL,
    INDEX reminders_due (done, due),
    INDEX reminders_user (user, done)
);
