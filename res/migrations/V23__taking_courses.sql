CREATE TABLE IF NOT EXISTS taking_courses (
    user  BIGINT      NOT NULL,
    code  VARCHAR(12) NOT NULL,
    since BIGINT      NOT NULL,
    PRIMARY KEY (user, code),
    INDEX by_course (code)
);
