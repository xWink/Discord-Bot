CREATE TABLE IF NOT EXISTS bang_results (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user      BIGINT  NOT NULL,
    played_at BIGINT  NOT NULL,
    died      TINYINT NOT NULL,
    jammed    TINYINT NOT NULL,
    KEY idx_bang_results_time (played_at),
    KEY idx_bang_results_user (user, played_at)
);
