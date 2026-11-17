CREATE TABLE IF NOT EXISTS question_bank (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    fingerprint   CHAR(64)     NOT NULL,
    provider      VARCHAR(40)  NOT NULL,
    kind          VARCHAR(10)  NOT NULL,
    category      VARCHAR(80)  NOT NULL,
    difficulty    VARCHAR(10)  NOT NULL,
    prompt        TEXT         NOT NULL,
    choices       TEXT         NOT NULL,
    correct_index INT          NOT NULL,
    explanation   TEXT         NULL,
    license       VARCHAR(40)  NOT NULL,
    attribution   VARCHAR(200) NOT NULL,
    created_at    BIGINT       NOT NULL,
    UNIQUE KEY uq_question_fp (fingerprint),
    KEY idx_question_kind (kind, category, difficulty)
);

CREATE TABLE IF NOT EXISTS question_refills (
    cache_key    CHAR(64) PRIMARY KEY,
    last_refill  BIGINT NOT NULL,
    last_success BIGINT NOT NULL DEFAULT 0,
    failures     INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS arcade_scores (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user        BIGINT  NOT NULL,
    question_id BIGINT  NOT NULL,
    correct     TINYINT NOT NULL,
    elapsed_ms  INT     NOT NULL,
    week        INT     NOT NULL,
    played_at   BIGINT  NOT NULL,
    KEY idx_scores_week (week, user),
    KEY idx_scores_user (user, id)
);

CREATE TABLE IF NOT EXISTS arcade_days (
    user BIGINT NOT NULL,
    day  INT    NOT NULL,
    week INT    NOT NULL,
    PRIMARY KEY (user, day),
    KEY idx_days_week (user, week)
);
