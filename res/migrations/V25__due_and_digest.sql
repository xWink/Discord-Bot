CREATE TABLE IF NOT EXISTS due_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user        BIGINT       NOT NULL,
    code        VARCHAR(12)  NOT NULL,
    title       VARCHAR(80)  NOT NULL,
    due         BIGINT       NOT NULL,
    done        TINYINT      NOT NULL DEFAULT 0,
    reminder_id BIGINT       NULL,
    created     BIGINT       NOT NULL,
    INDEX by_user (user, done, due)
);

CREATE TABLE IF NOT EXISTS digest_prefs (
    user          BIGINT  PRIMARY KEY,
    enabled       TINYINT NOT NULL DEFAULT 0,
    minute_of_day INT     NOT NULL DEFAULT 480,
    last_sent_day BIGINT  NOT NULL DEFAULT 0,
    INDEX by_enabled (enabled, minute_of_day)
);
