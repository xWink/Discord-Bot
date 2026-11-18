CREATE TABLE IF NOT EXISTS course_ratings (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user       BIGINT       NOT NULL,
    code       VARCHAR(12)  NOT NULL,
    difficulty TINYINT      NOT NULL,
    workload   TINYINT      NOT NULL,
    comment    VARCHAR(300) NULL,
    created    BIGINT       NOT NULL,
    hidden     TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY one_per_person (user, code),
    INDEX by_course (code, hidden)
);

CREATE TABLE IF NOT EXISTS course_rating_reports (
    rating_id BIGINT NOT NULL,
    reporter  BIGINT NOT NULL,
    PRIMARY KEY (rating_id, reporter)
);
